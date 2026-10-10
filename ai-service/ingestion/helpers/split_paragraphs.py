import logging

logger = logging.getLogger("ingestion")

def split_paragraphs(text, max_len=400):
    if not text:
        logger.warning("Empty text provided to split_paragraphs")
        return []

    sentences = text.split(". ")
    out = []
    buf = ""

    for sentence in sentences:
        sentence = sentence.strip()
        if not sentence:
            continue

        # Trường hợp 1: câu dài hơn max_len → cắt thông minh
        while len(sentence) > max_len:
            # tìm dấu chấm gần nhất trước max_len
            cut = sentence.rfind(". ", 0, max_len)

            if cut == -1:
                # fallback: không có dấu chấm → cắt cứng
                cut = max_len

            chunk = sentence[:cut].strip()
            if chunk:
                out.append(chunk)

            sentence = sentence[cut:].strip()

        # Trường hợp 2: ghép câu vào buffer
        if len(buf) + len(sentence) + 2 <= max_len:
            buf += sentence + ". "
        else:
            out.append(buf.strip())
            buf = sentence + ". "

    # flush buffer cuối
    if buf:
        out.append(buf.strip())

    return out
