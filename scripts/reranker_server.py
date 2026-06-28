import os
import sys
from flask import Flask, request, jsonify

# Prevent downloading large files if they are already cached in a specific directory (optional, standard huggingface cache is fine)
# Load transformers model
try:
    from transformers import AutoTokenizer, AutoModelForSequenceClassification
    import torch
except ImportError:
    print("Dependencies not met. Make sure transformers and torch are installed.")
    sys.exit(1)

app = Flask(__name__)

model_name = "BAAI/bge-reranker-large"
tokenizer = None
model = None

def get_model():
    global tokenizer, model
    if model is None:
        print(f"Loading reranker model: {model_name} on CPU...")
        tokenizer = AutoTokenizer.from_pretrained(model_name)
        model = AutoModelForSequenceClassification.from_pretrained(model_name)
        model.eval()
        print("Model loaded successfully.")
    return tokenizer, model

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "healthy", "model": model_name})

@app.route('/rerank', methods=['POST'])
def rerank():
    try:
        tok, mod = get_model()
    except Exception as e:
        return jsonify({"error": f"Failed to load model: {str(e)}"}), 500

    data = request.json
    if not data:
        return jsonify({"error": "Missing JSON body"}), 400

    query = data.get("query")
    documents = data.get("documents")

    if not query or not documents:
        return jsonify([])

    try:
        # Build query-document pairs
        pairs = [[query, doc] for doc in documents]
        
        with torch.no_grad():
            inputs = tok(pairs, padding=True, truncation=True, return_tensors='pt', max_length=512)
            # Run model inference on CPU
            logits = mod(**inputs).logits
            scores = logits.view(-1).float().tolist()
        
        return jsonify(scores)
    except Exception as e:
        return jsonify({"error": f"Reranking computation error: {str(e)}"}), 500

if __name__ == '__main__':
    # Start on localhost:8090
    print("Starting reranker server on http://127.0.0.1:8090...")
    app.run(host='127.0.0.1', port=8090, debug=False)
