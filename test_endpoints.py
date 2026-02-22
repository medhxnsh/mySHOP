import urllib.request
import json
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("http://localhost:8080/api/v1/auth/register", data=json.dumps({"fullName":"Tester User","email":"test10@example.com","password":"Password123!","role":"ADMIN"}).encode('utf-8'), headers={'Content-Type': 'application/json'})
try:
    response = urllib.request.urlopen(req, context=ctx)
    token = json.loads(response.read().decode('utf-8'))['data']['accessToken']
    print("Registered and got Token!")
except Exception as e:
    req = urllib.request.Request("http://localhost:8080/api/v1/auth/login", data=json.dumps({"email":"test10@example.com","password":"Password123!"}).encode('utf-8'), headers={'Content-Type': 'application/json'})
    response = urllib.request.urlopen(req, context=ctx)
    token = json.loads(response.read().decode('utf-8'))['data']['accessToken']
    print("Logged in and got Token!")

# Review test
req = urllib.request.Request("http://localhost:8080/api/v1/products/6110e93c-7064-43c3-b9df-cd136928cccd/reviews", data=json.dumps({"rating":5,"title":"test","comment":"test"}).encode('utf-8'), headers={'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token})
try:
    res = urllib.request.urlopen(req, context=ctx)
    print("REVIEW STATUS:", res.getcode())
    print("REVIEW BODY:", res.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("REVIEW HTTPError:", e.code)
    print("REVIEW ERROR BODY:", e.read().decode('utf-8'))

# Notification test
req = urllib.request.Request("http://localhost:8080/api/v1/notifications", headers={'Authorization': 'Bearer ' + token})
try:
    res = urllib.request.urlopen(req, context=ctx)
    print("NOTIF STATUS:", res.getcode())
    print("NOTIF BODY:", res.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("NOTIF HTTPError:", e.code)
    print("NOTIF ERROR BODY:", e.read().decode('utf-8'))

