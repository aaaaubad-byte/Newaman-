#!/usr/bin/env python3
"""
Live Core Workflow Contract Tests for AMAN (أمان)
Uses live Supabase credentials from environment.
"""

import os
import sys
import uuid
import json
import urllib.request
import urllib.error

SUPABASE_URL = os.environ.get("VITE_SUPABASE_URL", "https://ilhgwludktlktzwxryhs.supabase.co").rstrip("/")
ANON_KEY = os.environ.get("VITE_SUPABASE_ANON_KEY", "").strip()

CUSTOMER_EMAIL = "customer1@aman.ye"
CUSTOMER_PASS = "Customer@123456"

ADMIN_EMAIL = "manager@aman.ye"
ADMIN_PASS = "Manager@123456"

def http_req(url, method="GET", data=None, headers=None):
    if headers is None:
        headers = {}
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode("utf-8")
            status = response.status
            try:
                res_json = json.loads(res_body)
            except Exception:
                res_json = res_body
            return status, res_json
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            err_json = json.loads(err_body)
        except Exception:
            err_json = err_body
        return e.code, err_json

def login(email, password):
    url = f"{SUPABASE_URL}/auth/v1/token?grant_type=password"
    headers = {"apikey": ANON_KEY}
    status, data = http_req(url, "POST", {"email": email, "password": password}, headers)
    if status != 200:
        print(f"Failed to login {email}: {status} {data}")
        sys.exit(1)
    return data["access_token"], data["user"]["id"]

def rpc(fn_name, payload, token):
    url = f"{SUPABASE_URL}/rest/v1/rpc/{fn_name}"
    headers = {
        "apikey": ANON_KEY,
        "Authorization": f"Bearer {token}"
    }
    return http_req(url, "POST", payload, headers)

def run_tests():
    print("=== AMAN Live Core Contracts Test Suite ===")
    print(f"Supabase URL: {SUPABASE_URL}")
    print(f"Anon Key Present: {bool(ANON_KEY)}")
    
    # 1. Login actors
    print("\n[1] Authenticating Customer and Admin...")
    cust_token, cust_id = login(CUSTOMER_EMAIL, CUSTOMER_PASS)
    admin_token, admin_id = login(ADMIN_EMAIL, ADMIN_PASS)
    print(f"  Customer ID: {cust_id}")
    print(f"  Admin ID: {admin_id}")
    
    # Fetch a valid package and wallet
    headers_admin = {"apikey": ANON_KEY, "Authorization": f"Bearer {admin_token}"}
    _, pkg_res = http_req(f"{SUPABASE_URL}/rest/v1/packages?is_active=eq.true&limit=1", "GET", headers=headers_admin)
    _, wallet_res = http_req(f"{SUPABASE_URL}/rest/v1/payment_methods?is_active=eq.true&limit=1", "GET", headers=headers_admin)
    
    assert len(pkg_res) > 0, "No active packages found"
    assert len(wallet_res) > 0, "No active payment methods found"
    package_id = pkg_res[0]["id"]
    wallet_id = wallet_res[0]["id"]
    print(f"  Using Package: {package_id} ({pkg_res[0]['name']})")
    print(f"  Using Wallet: {wallet_id} ({wallet_res[0]['name']})")

    # 2. Add Customer Number
    test_number = f"77{uuid.uuid4().int % 10000000:07d}"
    print(f"\n[2] Registering test phone number: {test_number}...")
    status, number_data = rpc("add_customer_number", {"p_number": test_number}, cust_token)
    assert status == 200, f"Failed to add number: {status} {number_data}"
    number_id = number_data["id"]
    print(f"  Success: number_id = {number_id}, provider = {number_data.get('provider_name_ar')}")

    # 3. Submit Protection Request & Idempotency
    print("\n[3] Testing Submit Protection Request & Idempotency...")
    idempotency_key = str(uuid.uuid4())
    ref1 = f"REF-{uuid.uuid4().hex[:8]}"
    submit_payload = {
        "p_number_id": number_id,
        "p_package_id": package_id,
        "p_wallet_id": wallet_id,
        "p_transfer_ref": ref1,
        "p_idempotency_key": idempotency_key
    }
    
    # First submit
    status, req_id = rpc("submit_protection_request", submit_payload, cust_token)
    assert status == 200, f"Initial submission failed: {status} {req_id}"
    print(f"  Initial submission created request: {req_id}")

    # Idempotent replay
    status_idemp, req_idemp = rpc("submit_protection_request", submit_payload, cust_token)
    assert status_idemp == 200, f"Idempotent replay failed: {status_idemp} {req_idemp}"
    assert req_idemp == req_id, "Idempotent replay did not return original request ID"
    print("  Idempotent retry verified successfully.")

    # Duplicate submission (different ref/idempotency while pending)
    status_dup, err_dup = rpc("submit_protection_request", {
        "p_number_id": number_id,
        "p_package_id": package_id,
        "p_wallet_id": wallet_id,
        "p_transfer_ref": f"REF-DUP-{uuid.uuid4().hex[:6]}",
        "p_idempotency_key": str(uuid.uuid4())
    }, cust_token)
    assert status_dup in [400, 409], f"Duplicate pending submission should be rejected: {status_dup} {err_dup}"
    print(f"  Duplicate pending request correctly blocked: {err_dup}")

    # 4. Admin Verifies & Approves Protection Request
    print("\n[4] Admin Approves Protection Request...")
    status_app, app_data = rpc("verify_and_approve_protection_request", {"p_request_id": req_id}, admin_token)
    assert status_app == 200, f"Approval failed: {status_app} {app_data}"
    protection_id = app_data["protection_id"]
    print(f"  Protection created successfully: {protection_id}")

    # 5. Attempting to submit a new protection for already active number must be blocked
    print("\n[5] Testing duplicate submission against active protection...")
    status_active, err_active = rpc("submit_protection_request", {
        "p_number_id": number_id,
        "p_package_id": package_id,
        "p_wallet_id": wallet_id,
        "p_transfer_ref": f"REF-ACTIVE-{uuid.uuid4().hex[:6]}",
        "p_idempotency_key": str(uuid.uuid4())
    }, cust_token)
    assert status_active in [400, 409], f"Submitting protection on active number should fail: {status_active} {err_active}"
    print(f"  Conflict on active protection correctly blocked: {err_active}")

    # 6. Payment Tasks Lifecycle (Cancel and Complete guards)
    print("\n[6] Testing Payment Tasks lifecycle...")
    _, tasks_res = http_req(f"{SUPABASE_URL}/rest/v1/payment_tasks?protection_id=eq.{protection_id}&order=cycle_number.asc", "GET", headers=headers_admin)
    assert len(tasks_res) > 0, "No payment task generated for approved protection"
    task_id = tasks_res[0]["id"]
    print(f"  Generated payment task: {task_id}, status: {tasks_res[0]['status']}")

    # Cancel payment task
    status_cancel, cancel_res = rpc("cancel_payment_task", {"p_task_id": task_id, "p_reason": "اختبار الإلغاء الآلي"}, admin_token)
    assert status_cancel == 200, f"Cancel task failed: {status_cancel} {cancel_res}"
    print("  Payment task cancelled successfully.")

    # Attempt to complete a cancelled task -> MUST FAIL
    status_comp, err_comp = rpc("complete_payment_task", {"p_task_id": task_id, "p_telecom_ref": "TEL-REF-123"}, admin_token)
    assert status_comp in [400, 409], f"Completing cancelled task should fail: {status_comp} {err_comp}"
    print(f"  Completing cancelled task correctly blocked: {err_comp}")

    print("\n=== ALL CORE CONTRACT TESTS PASSED SUCCESSFULLY ===")

if __name__ == "__main__":
    run_tests()
