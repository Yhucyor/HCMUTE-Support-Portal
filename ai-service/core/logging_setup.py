import logging.config
import yaml

import os

def setup_logging():
    os.makedirs("logs", exist_ok=True)
    with open("config/logging.yaml", encoding="utf-8") as file:
        logging.config.dictConfig(yaml.safe_load(file)) # doc file yaml va chuyen sang python dict