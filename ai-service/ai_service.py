"""
AI Classification Microservice for Complynt
Primary: CoolHatt/distalBERT-BANK-COMPLAINS via HuggingFace Inference API
Fallback: Groq LLM (when confidence < 60%)
"""

from flask import Flask, request, jsonify
from groq import Groq
import os, json, logging, time, requests
from datetime import datetime

app = Flask(__name__)

# ── Logging Setup ─────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("classification.log")
    ]
)
log = logging.getLogger(__name__)

# ── Config ────────────────────────────────────────────────────────────────────
HF_API_TOKEN   = "Your token"
HF_MODEL_ID    = "CoolHatt/distalBERT-BANK-COMPLAINS"
HF_API_URL     = f"https://api-inference.huggingface.co/models/{HF_MODEL_ID}"
HF_HEADERS     = {"Authorization": f"Bearer {HF_API_TOKEN}"}

GROQ_API_KEY   = os.environ.get("GROQ_API_KEY", "Your Tokken")
groq_client    = Groq(api_key=GROQ_API_KEY)

CONFIDENCE_THRESHOLD = 0.60

# Consumer-complaint taxonomy (HF model). Java maps these to ProductType/IssueType enums — see CategoryMappingService.
KNOWN_LABELS = [
    "CREDIT_CARD", "RETAIL_BANKING", "CREDIT_REPORTING",
    "MORTGAGES_AND_LOANS", "DEBT_COLLECTION", "OTHER"
]


# ── HuggingFace Inference API call ────────────────────────────────────────────
def classify_with_hf(description: str) -> dict | None:
    """
    Calls HuggingFace Inference API.
    Returns { label, confidence } or None on failure.
    """
    try:
        t0 = time.time()
        response = requests.post(
            HF_API_URL,
            headers=HF_HEADERS,
            json={"inputs": description[:512]},
            timeout=15
        )
        elapsed = (time.time() - t0) * 1000

        # Model still loading on HF side
        if response.status_code == 503:
            log.warning(f"⏳ HF model is loading (503) — falling back to Groq")
            return None

        response.raise_for_status()
        raw = response.json()
        log.info(f"📨  HF raw response : {raw}")

        # HF returns: [[{"label": "...", "score": 0.92}, ...]]
        top = raw[0][0] if isinstance(raw[0], list) else raw[0]

        label      = top["label"]
        confidence = round(top["score"], 4)

        log.info(f"🔍  HF DistilBERT: {label} = {confidence:.4f}  ({elapsed:.1f} ms)")
        return {"label": label, "confidence": confidence, "elapsed": elapsed}

    except Exception as e:
        log.error(f"❌ HF Inference API error: {e}")
        return None


# ── Groq fallback ─────────────────────────────────────────────────────────────
def classify_with_groq(description: str) -> dict:
    prompt = f"""You are a bank complaint classifier. Classify the following complaint into EXACTLY one of these categories:
{", ".join(KNOWN_LABELS)}

Respond with a JSON object only — no explanation, no markdown. Format:
{{"label": "<CATEGORY>", "confidence": <0.0-1.0>}}

Complaint: {description[:512]}"""

    try:
        t0 = time.time()
        chat = groq_client.chat.completions.create(
            model="llama3-8b-8192",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.0,
            max_tokens=60,
        )
        elapsed = (time.time() - t0) * 1000

        raw    = chat.choices[0].message.content.strip()
        log.info(f"📨  Groq raw response : {raw}")
        parsed = json.loads(raw)

        label = parsed.get("label", "OTHER").upper()
        if label not in KNOWN_LABELS:
            log.warning(f"⚠️  Unknown label '{label}' → OTHER")
            label = "OTHER"

        confidence = float(parsed.get("confidence", 0.0))
        log.info(f"🧠  Groq: {label} = {confidence:.4f}  ({elapsed:.1f} ms)")
        return {"label": label, "confidence": round(confidence, 4), "source": "groq"}

    except Exception as e:
        log.error(f"❌ Groq error: {e}")
        return {"label": "OTHER", "confidence": 0.0, "source": "groq_error"}


# ── /classify endpoint ────────────────────────────────────────────────────────
@app.route("/classify", methods=["POST"])
def classify():
    data        = request.get_json(force=True)
    description = data.get("description", "")
    request_id  = datetime.now().strftime("%H%M%S%f")

    log.info("-" * 60)
    log.info(f"📥  Request ID : {request_id}")
    log.info(f"📝  Input      : {description[:120]}{'...' if len(description) > 120 else ''}")

    if not description.strip():
        log.warning("⚠️  Empty input → OTHER")
        return jsonify({"label": "OTHER", "confidence": 0.0, "source": "none"}), 200

    # ── Step 1: HuggingFace Inference API ────────────────────────────────────
    hf_result = classify_with_hf(description)

    if hf_result and hf_result["confidence"] >= CONFIDENCE_THRESHOLD:
        log.info(f"✅  [{request_id}] CLASSIFIED BY → HUGGINGFACE  ({hf_result['confidence']:.2%})")
        log.info("-" * 60)
        return jsonify({
            "label":      hf_result["label"],
            "confidence": hf_result["confidence"],
            "source":     "huggingface"
        })

    # ── Step 2: Groq fallback ─────────────────────────────────────────────────
    if hf_result:
        log.warning(f"⬇️  HF confidence {hf_result['confidence']:.2%} < {CONFIDENCE_THRESHOLD:.0%} → Groq")
    else:
        log.warning("⬇️  HF call failed → Groq")

    groq_result = classify_with_groq(description)
    log.info(f"✅  [{request_id}] CLASSIFIED BY → GROQ  ({groq_result['confidence']:.2%})")
    log.info("-" * 60)
    return jsonify(groq_result)


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status":    "ok",
        "primary":   f"HuggingFace API → {HF_MODEL_ID}",
        "fallback":  "groq/llama3-8b-8192",
        "threshold": CONFIDENCE_THRESHOLD
    })


if __name__ == "__main__":
    log.info("=" * 60)
    log.info("  Complynt AI Classifier — Starting on :8000")
    log.info(f"  HF Model  : {HF_MODEL_ID}")
    log.info(f"  Threshold : {CONFIDENCE_THRESHOLD:.0%}")
    log.info("=" * 60)
    app.run(host="0.0.0.0", port=8000, debug=False)