import os
import requests
from dotenv import load_dotenv

load_dotenv()

API_KEY = os.getenv("OPENAI_API_KEY")
MODEL = "gpt-5.5"
URL = "https://api.openai.com/v1/responses"

if not API_KEY:
    print("ERROR: API key missing in .env")
    raise SystemExit(1)

history = []

print("""
╔════════════════════════════╗
║       SMART AI 🤖          ║
║   Your Personal Assistant  ║
╚════════════════════════════╝
Type 'exit' to close.
""")

while True:
    try:
        question = input("\nYou: ").strip()

        if question.lower() in ("exit", "quit"):
            print("Smart AI: Goodbye! ❤️")
            break

        if not question:
            continue

        history.append({
            "role": "user",
            "content": question
        })

        payload = {
            "model": MODEL,
            "instructions": (
                "Your name is Smart AI. "
                "You are a helpful, intelligent multilingual assistant. "
                "Reply naturally in the user's language. "
                "Be friendly, accurate and concise."
            ),
            "input": history[-20:]
        }

        response = requests.post(
            URL,
            headers={
                "Authorization": f"Bearer {API_KEY}",
                "Content-Type": "application/json"
            },
            json=payload,
            timeout=120
        )

        if response.status_code != 200:
            history.pop()
            print("API Error:", response.status_code)
            print(response.text[:1500])
            continue

        data = response.json()
        answer = ""

        for item in data.get("output", []):
            if item.get("type") == "message":
                for part in item.get("content", []):
                    if part.get("type") == "output_text":
                        answer += part.get("text", "")

        if answer:
            history.append({
                "role": "assistant",
                "content": answer
            })
            print("\nSmart AI:", answer)
        else:
            history.pop()
            print("No text response received.")

    except KeyboardInterrupt:
        print("\nSmart AI closed.")
        break
    except requests.RequestException as error:
        print("Connection error:", error)
        if history and history[-1]["role"] == "user":
            history.pop()
