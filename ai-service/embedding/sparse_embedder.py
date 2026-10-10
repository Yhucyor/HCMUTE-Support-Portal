import math
import logging
import re
from collections import Counter

logger = logging.getLogger("embedding")

def tokenize(text: str) -> list[str]: # Nhan ve 1 doan text thanh 1 list cac token cua keyword
    if not text:
        logger.warning("Empty text received for tokenization in sparse_embedder.")
        return []
    
    text = text.lower() # Chuyen thanh chu thuong de giam so luong token khac nhau. VD: "Apple" va "apple" se duoc xem la giong nhau
    text = re.sub(r"[^\w\s]", " ", text) # Xoa ky tu dac biet. VD: "hello, world!" -> "hello world". 
    # [^\w\s] la bieu thuc chinh quy de tim kiem ky tu khong phai chu cai hoac so hoac khoang trang
    tokens = text.split()
    return tokens

class SparseEmbedder:
    def __init__(self):
        self.vocabulary: dict[str, int] = {} # Tu dien de luu tru cac token va chi so tuong ung cua chung
        self.document_frequency: Counter = Counter() # Dem so luong van ban chua token do
        self.num_documents = 0 # Tong so van ban da duoc su dung de fit model
    
    def __update_vocabulary(self, tokens: list[str]): # Dùng để xây dựng vocabulary và document frequency
        """
            Kết quả nhận được :
               Vocabulary:
                     {
                          "du": 0,
                          "an": 1,
                          "nha": 2,
                          "dep": 3,
                          "thiet": 4,
                          "ke": 5,
                            ...
                     }
                     
                Document Frequency:
                        {
                            "du": 1,
                            "an": 1,
                            "nha": 2,
                            "dep": 1,
                            "thiet": 1,
                            "ke": 1,
                                ...
                        }
                        
        ["biệt" : 0, "thự" : 1, "nhà" : 2, "phố" : 3, "hiện" : 4, "đại" : 5, "quận" : 6, "3" : 7]
        """
        for token in set(tokens): # Dung set de tranh dem trung token trong van ban
            self.document_frequency[token] += 1 # Tang so lan xuat hien cua token trong cac van ban. VI du: token "nha" xuat hien trong 2 van ban thi document_frequency["nha"] = 2
            if token not in self.vocabulary: # Neu token chua co trong tu dien
                self.vocabulary[token] = len(self.vocabulary) # Gan chi so moi cho token moi. Vi du: neu vocabulary dang co 5 token, thi token moi se duoc gan chi so la 5
                
    def fit(self, texts: list[str]): # Fit model voi danh sach van ban texts chua cac text
        """
            Vi du:
                texts = [
                    "nha pho hien dai",
                    "thiet ke nha dep",
                    "nha cao cap hien dai"
                ]
                
            num_documents = 3 (len(texts))
            
            Lấy từng text trong text
            Ví dụ :
                text = "nha pho hien dai"
                tokens = ["nha", "pho", "hien", "dai"]
                Cập nhật vocabulary và document frequency với tokens trên
        """
        self.num_documents = len(texts) # Cap nhat tong so van ban, tuc la tong cac chunk trong texts
        for text in texts: 
            tokens = tokenize(text) # Tach van ban thanh cac token
            self.__update_vocabulary(tokens) # Cap nhat tu dien va dem tan so van ban

        logger.info(f"Fitted Sparse Embedder with {self.num_documents} documents and vocabulary size {len(self.vocabulary)}.")
        
    def __inverse_document_frequency(self, term:str) -> float: # inverse document frequency. term la token can tinh idf
        document_frequency = self.document_frequency.get(term, 0)  # Lay so van ban chua term, neu khong co thi tra ve 0
        return math.log((self.num_documents + 1) / (document_frequency + 1)) + 1 # Cong 1 de tranh chia cho 0
    
    def encode(self, text:str) -> dict[str, list[float]]: # Tra ve 1 dict chua 2 list: indices va values
        """
            Vi du:
                text = "nha dep hien dai"
                tokens = ["nha", "dep", "hien", "dai", "nha"]
                term_frequency = {
                    "nha": 2,
                    "dep": 1,
                    "hien": 1,
                    "dai": 1
                }
                
                Giả sử trong vocabulary ta có:
                    {`"nha": 0, "dep": 1, "hien": 2, "dai": 3, ...}
                
                Giả sử ta có document_frequency:
                    {`"nha": 2, "dep": 1, "hien": 3, "dai": 2, ...}
                    
                num_documents = 5
                indices = [0, 1, 2, 3]
                values = [
                    2 * idf("nha"),
                    1 * idf("dep"),
                    1 * idf("hien"),
                    1 * idf("dai")
                ]
        """
        tokens = tokenize(text) # Tach van ban thanh cac token
        if not tokens:
            logger.warning("Empty token list after tokenization in encode method.")
            return {"indices": [], "values": []}
        
        term_frequency = Counter(tokens) # Dem tan so xuat hien cua tung token trong van ban
        indices = []
        values = []
        
        for term, frequency in term_frequency.items(): # Duyet qua tung token va tan so cua no
            if term not in self.vocabulary:
                logger.debug(f"Term '{term}' not found in vocabulary during encoding. Skipping.") 
                continue
            
            term_id = self.vocabulary[term] # Lay chi so cua token trong tu dien
            weight = frequency * self.__inverse_document_frequency(term) # Tinh trong so bang tan so * idf
            
            indices.append(term_id) # Them chi so vao danh sach indices
            values.append(float(weight)) # Them trong so vao danh sach values
            
        return {"indices": indices, "values": values}
    
    def encode_batch(self, texts: list[str]) -> list[dict[str, list[float]]]: # Ma hoa 1 loat van ban
        return [self.encode(text) for text in texts] # Su dung list comprehension de goi phuong thuc encode cho tung van ban trong texts