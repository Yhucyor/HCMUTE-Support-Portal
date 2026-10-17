import json
import logging
from pathlib import Path
from datetime import datetime

from core.settings_loader import load_settings
from ingestion.helpers.make_metadata import make_metadata

settings = load_settings()
logger = logging.getLogger("ingestion")

def chunk_company_info():
    # khai bao file path
    file_path = Path(settings["data"]["processed_dir"]) / "companyInfo.json"
    
    if not file_path.exists():
        logger.error(f"File not found: {file_path}")
        return []
    
    # doc du lieu tu file json
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            company_info = json.load(f)
    # neu file json khong hop le thi log loi. Vi du: , cuois cau, thieu ngoac,....
    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON format {e}")
        return []
    # neu khong co du lieu thi log loi
    except Exception as e:
        logger.error(f"Failed to load {e}")
        return []
    
    # neu du lieu la dict thi chuyen thanh list chua dict. Vi du: { "companyName": "Company A", "description": "..." } => [ { "companyName": "Company A", "description": "..." } ]
    if isinstance(company_info, dict):
        company_info = [company_info]
        
    # kiem tra kieu du lieu co phai la list khong. Vi du: [ { "name": "Company A", "description": "..." }, { "name": "Company B", "description": "..." } ]
    if not isinstance(company_info, list):
        logger.error("Company info data is not a list")
        return []
    
    if not company_info: # kiem tra danh sach co rong khong. Vi du: []
        logger.warning("No company info found in the data")
        return []
    
    chunks = [] # danh sach luu cac doan van ban
    
    for idx, company in enumerate(company_info):
        if not isinstance(company, dict):
            logger.warning(f"Invalid company info at index {idx}: expected a dictionary")
            continue
        
        company_id = company.get("id") # lay id cong ty
        company_name = company.get("companyName", "") # lay ten cong ty
        company_slogan = company.get("companySlogan") # lay slogan cong ty
        company_description = company.get("companyDescription", "") # lay mo ta cong ty
        company_hotline = company.get("hotlines") # lay hotline cong ty        
        company_email = company.get("emails") # lay email cong ty
        company_address = company.get("mainAddress") # lay dia chi cong ty
        company_working_hours = company.get("workingHours") # lay gio lam viec cong ty
        company_website = company.get("website") # lay website cong ty
        company_total_projects = company.get("totalProjects") # lay tong so du an cong ty da thuc hien
        company_social_links = company.get("socialLinks") # lay cac link mang xa hoi cong ty
        company_social_text = "" # khoi tao bien luu chuoi cac mang xa hoi cua cong ty
        if isinstance(company_social_links, dict): # kiem tra kieu du lieu co phai la dict khong. Vi du: { "facebook": "fb.com/xyz", "instagram": "insta.com/xyz" }
            # chuyen doi dict thanh chuoi. Vi du: "facebook: fb.com/xyz, instagram: insta.com/xyz". Neu gia tri rong thi khong them vao chuoi
            company_social_text = ", ".join([f"{key}: {value}" for key, value in company_social_links.items() if value]) 
        
        base_metadata = {
            "type": "company_info",
            "company_id": company_id,
            "company_name": company_name,
            "source": "companyInfo.json",
            "created_at": datetime.utcnow().isoformat(),
            "language": "vi"
        }
        
        CHUNK_PRIORITY = {
            "overview": 1,
            "description": 2,
            "contact_info": 3,
        }
        
        # Chunk overview
        if company_name and company_slogan:
            chunks.append({
                "text": f'Tên công ty: {company_name}\nKhẩu hiệu công ty: {company_slogan}',
                "metadata": make_metadata(base_metadata, chunk_type="overview", priority=CHUNK_PRIORITY["overview"])
            })
            
        # Chunk full description
        if company_description:
            if company_total_projects is not None:
                chunks.append({
                    "text": f"Mô tả công ty {company_name}: {company_description}. \nCông ty đã thực hiện tổng cộng {company_total_projects} dự án." ,
                    "metadata": make_metadata(base_metadata, chunk_type="description", priority=CHUNK_PRIORITY["description"])
                })
            else:
                chunks.append({
                    "text": f"Mô tả công ty {company_name}: {company_description}.",
                    "metadata": make_metadata(base_metadata, chunk_type="description", priority=CHUNK_PRIORITY["description"])
                })
            
        # Chunk contact info
        if company_hotline or company_email or company_address or company_website or company_social_text:
            text_parts = []
            if company_hotline:
                text_parts.append(f"Hotline: {company_hotline}")
            if company_email:
                text_parts.append(f"Email: {company_email}")
            if company_address:
                text_parts.append(f"Địa chỉ: {company_address}")
            if company_working_hours:
                text_parts.append(f"Giờ làm việc: {company_working_hours}")
            if company_website:
                text_parts.append(f"Website: {company_website}")
            if company_social_text:
                text_parts.append(f"Mạng xã hội: {company_social_text}")
            chunks.append({
                "text": "\n".join(text_parts),
                "metadata": make_metadata(base_metadata, chunk_type="contact_info", priority=CHUNK_PRIORITY["contact_info"])
            })
        
    return chunks