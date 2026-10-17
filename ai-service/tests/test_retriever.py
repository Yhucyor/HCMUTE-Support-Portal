import unittest
from unittest.mock import Mock, patch, MagicMock
from retrieval.retriever import retrieve

class TestRetriever(unittest.TestCase):
    
    def test_empty_query(self):
        """Test với query rỗng"""
        result = retrieve("")
        self.assertEqual(result, [])
    
    def test_whitespace_query(self):
        """Test với query chỉ có khoảng trắng"""
        result = retrieve("   ")
        self.assertEqual(result, [])
    
    @patch('retrieval.retriever.get_qdrant_client')
    @patch('retrieval.retriever.embed_texts')
    def test_embedding_failure(self, mock_embed, mock_client):
        """Test khi embedding thất bại"""
        mock_embed.return_value = []
        result = retrieve("test query")
        self.assertEqual(result, [])
    
    @patch('retrieval.retriever.get_qdrant_client')
    @patch('retrieval.retriever.embed_texts')
    def test_successful_retrieval(self, mock_embed, mock_client):
        """Test retrieval thành công"""
        # Mock embedding
        mock_embed.return_value = [[0.1, 0.2, 0.3]]
        
        # Mock Qdrant response
        mock_point = Mock()
        mock_point.id = "test-id"
        mock_point.score = 0.95
        mock_point.payload = {
            "text": "Test content",
            "source": "test"
        }
        
        mock_response = Mock()
        mock_response.points = [mock_point]
        
        mock_client_instance = Mock()
        mock_client_instance.query_points.return_value = mock_response
        mock_client.return_value = mock_client_instance
        
        result = retrieve("test query")
        
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0].text, "Test content")
        self.assertEqual(result[0].score, 0.95)
    
    @patch('retrieval.retriever.get_qdrant_client')
    @patch('retrieval.retriever.embed_texts')
    def test_qdrant_connection_error(self, mock_embed, mock_client):
        """Test xử lý lỗi kết nối Qdrant"""
        from qdrant_client.http.exceptions import ResponseHandlingException
        
        mock_embed.return_value = [[0.1, 0.2, 0.3]]
        mock_client.side_effect = ResponseHandlingException("Connection error")
        
        with self.assertRaises(ConnectionError):
            retrieve("test query")

if __name__ == '__main__':
    unittest.main()
