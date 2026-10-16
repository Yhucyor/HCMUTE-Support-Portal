from dataclasses import dataclass

# schema duoc dung de dinh nghia cau truc cua tai lieu duoc truy xuat tu he thong truy van.
@dataclass
class RetrievedDocument:
    id: str
    score: float
    text: str
    metadata: dict[str, any]