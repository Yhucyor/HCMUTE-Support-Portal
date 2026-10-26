import os
import sys
import pymysql
import cloudinary
import cloudinary.uploader

# Đảm bảo in UTF-8 không lỗi trên Windows
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

# 1. Cấu hình Cloudinary
cloudinary.config(
    cloud_name="db9j7uhdf",
    api_key="818745529863945",
    api_secret="nw79tJ47LQNbD6L0ZR38wJU6awo",
    secure=True
)

# 2. Kết nối TiDB Cloud
conn = pymysql.connect(
    host="gateway01.ap-northeast-1.prod.aws.tidbcloud.com",
    port=4000,
    user="2DAAeHS9ypVrW2e.root",
    password="fFkcxwPIDBS8tgOs",
    database="test",
    ssl={"ca": None},
    autocommit=True
)
cur = conn.cursor()

print("==================================================================")
print("🚀 BẮT ĐẦU ĐỒNG BỘ MEDIA LÊN CLOUDINARY CHO QAUTE PORTAL")
print("==================================================================")

# --- PHẦN 1: TẢI 20 CÔNG VĂN / QUY ĐỊNH (PDF) ---
print("\n--- 1. UPLOAD 20 CÔNG VĂN QUY ĐỊNH (PDF) ---")
cur.execute("SELECT id, post_id, file_name, file_url FROM post_attachments WHERE post_id >= 30000 ORDER BY id ASC")
regulations = cur.fetchall()

for row in regulations:
    att_id, post_id, file_name, local_path = row
    if os.path.exists(local_path):
        size = os.path.getsize(local_path)
        print(f"Uploading [{att_id}] {file_name} ({size} bytes)...", end=" ", flush=True)
        try:
            res = cloudinary.uploader.upload(
                local_path,
                folder="qaute_portal/regulations",
                resource_type="auto",
                use_filename=True,
                unique_filename=True
            )
            cloud_url = res.get("secure_url")
            cur.execute("UPDATE post_attachments SET file_url = %s, file_size = %s WHERE id = %s", (cloud_url, size, att_id))
            print(f"DONE -> {cloud_url}")
        except Exception as e:
            print(f"FAILED: {e}")
    else:
        print(f"WARNING: File not found locally: {local_path}")

# --- PHẦN 2: TẢI ĐÍNH KÈM TICKET / CHAT TƯ VẤN VIÊN ---
print("\n--- 2. UPLOAD TỆP ĐÍNH KÈM TICKET / CHAT ---")
cur.execute("SELECT id, ticket_id, file_name, file_url FROM attachments")
attachments = cur.fetchall()

upload_image_dir = r"C:\upload\images"

for row in attachments:
    att_id, ticket_id, file_name, file_url = row
    fname = file_url.split("fname=")[-1] if "fname=" in file_url else file_url
    local_path = os.path.join(upload_image_dir, fname)
    if os.path.exists(local_path):
        size = os.path.getsize(local_path)
        print(f"Uploading Ticket #{ticket_id} attachment [{att_id}] {file_name}...", end=" ", flush=True)
        try:
            res = cloudinary.uploader.upload(
                local_path,
                folder="qaute_portal/tickets",
                resource_type="auto",
                use_filename=True,
                unique_filename=True
            )
            cloud_url = res.get("secure_url")
            cur.execute("UPDATE attachments SET file_url = %s, file_size = %s WHERE id = %s", (cloud_url, size, att_id))
            print(f"DONE -> {cloud_url}")
        except Exception as e:
            print(f"FAILED: {e}")
    else:
        print(f"File {local_path} not found")

# --- PHẦN 3: TẢI AVATAR ADMIN ---
print("\n--- 3. UPLOAD AVATAR ADMIN ---")
cur.execute("SELECT id, username, avatar_url FROM users WHERE id = 1")
admin_user = cur.fetchone()
if admin_user:
    u_id, username, avatar_url = admin_user
    fname = avatar_url.split("fname=")[-1] if "fname=" in avatar_url else avatar_url
    local_path = os.path.join(upload_image_dir, fname)
    if os.path.exists(local_path):
        print(f"Uploading avatar for user {username}...", end=" ", flush=True)
        try:
            res = cloudinary.uploader.upload(
                local_path,
                folder="qaute_portal/avatars",
                resource_type="image",
                use_filename=True,
                unique_filename=True
            )
            cloud_url = res.get("secure_url")
            cur.execute("UPDATE users SET avatar_url = %s WHERE id = %s", (cloud_url, u_id))
            print(f"DONE -> {cloud_url}")
        except Exception as e:
            print(f"FAILED: {e}")

conn.close()
print("\n==================================================================")
print("🎉 HOÀN TẤT ĐỒNG BỘ TẤT CẢ MEDIA LÊN CLOUDINARY!")
print("==================================================================")
