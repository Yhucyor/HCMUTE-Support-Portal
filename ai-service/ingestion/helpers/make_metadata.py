import uuid

def make_metadata(base, **extra):
    return {
        **base,
        "chunk_id": str(uuid.uuid4()),
        **extra
    }