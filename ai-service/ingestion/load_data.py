import json
import logging
from pathlib import Path

from core.settings_loader import load_settings

settings = load_settings()
logger = logging.getLogger("ingestion")

def load_data():
    data = json.load(open(Path(settings["data"]["raw_dir"]) / "database_export_2026-01-14T02-32-14.json", "r", encoding="utf-8"))
    
    if not data:
        logger.error("No raw data found")
        return
    
    tables = data.get("tables", {})
    
    if not tables:
        logger.warning("No tables found")
        return
    
    # table_name: ten bang, table_data: du lieu trong bang
    for table_name, table_data in tables.items(): 
        if not table_data:
            logger.warning(f"No data for table {table_name}")
            continue
        
        # tao duong dan file json moi cho tung bang
        output_path = Path(settings["data"]["processed_dir"]) / f"{table_name}.json" 
        
        # mo file de ghi du lieu
        with open(output_path, "w", encoding="utf-8") as outfile: 
            # ensure_ascii=False de giu nguyen tieng viet, indent=4 de format lai file de doc hon
            json.dump(table_data, outfile, ensure_ascii=False, indent=4) 
            
        logger.info(f"Data for table {table_name} written to {output_path}")
        
if __name__ == "__main__":
    load_data()