import urllib.request
import json
import uuid

url = "https://551p9g.api.infobip.com/whatsapp/1/message/template"

payload_dict = {
  "messages": [
    {
      "from": "447860088970",
      "to": "919999999999",
      "messageId": str(uuid.uuid4()),
      "content": {
        "templateName": "test_whatsapp_template_en",
        "templateData": {
          "body": {
            "placeholders": [
              "Suresh"
            ]
          }
        },
        "language": "en"
      }
    }
  ]
}

data = json.dumps(payload_dict).encode('utf-8')

req = urllib.request.Request(url, data=data, method="POST")
req.add_header('Authorization', 'App bb770f9639c68f98c87c9ed3112db021-434a699c-3e18-468b-9311-d55e9ee7647e')
req.add_header('Content-Type', 'application/json')
req.add_header('Accept', 'application/json')

try:
    with urllib.request.urlopen(req) as response:
        print("Status Code:", response.status)
        print("Response:", response.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("HTTP Error:", e.code)
    print("Response body:", e.read().decode('utf-8'))
except Exception as e:
    print("Error:", str(e))
