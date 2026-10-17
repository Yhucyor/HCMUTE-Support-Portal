import unittest
from unittest.mock import Mock, patch
from api.routes.chat import chat

class TestChatRoute(unittest.TestCase):
    
    def test_empty_question(self):
        """Test xử lý câu hỏi rỗng"""
        result = chat("")
        self.assertEqual(result, "Vui lòng nhập câu hỏi.")
    
    def test_whitespace_question(self):
        """Test xử lý câu hỏi chỉ có khoảng trắng"""
        result = chat("   ")
        self.assertEqual(result, "Vui lòng nhập câu hỏi.")
    
    def test_query_too_long(self):
        """Test xử lý câu hỏi quá dài"""
        long_query = "a" * 600  # > 500 chars
        result = chat(long_query)
        self.assertIn("quá dài", result)
    
    @patch('api.routes.chat.retrieve')
    def test_no_documents_retrieved(self, mock_retrieve):
        """Test khi không tìm thấy documents"""
        mock_retrieve.return_value = []
        result = chat("test question")
        self.assertIn("không tìm thấy", result)
    
    @patch('api.routes.chat.retrieve')
    @patch('api.routes.chat.generate_answer')
    def test_successful_chat(self, mock_generate, mock_retrieve):
        """Test flow thành công"""
        # Mock retrieved documents
        mock_doc = Mock()
        mock_doc.text = "Test content"
        mock_doc.metadata = {"source": "test"}
        mock_retrieve.return_value = [mock_doc]
        
        # Mock generated answer
        mock_generate.return_value = "Test answer"
        
        result = chat("test question")
        self.assertEqual(result, "Test answer")
        mock_retrieve.assert_called_once()
        mock_generate.assert_called_once()
    
    @patch('api.routes.chat.retrieve')
    def test_exception_handling(self, mock_retrieve):
        """Test xử lý exception"""
        mock_retrieve.side_effect = Exception("Test error")
        result = chat("test question")
        self.assertIn("lỗi", result.lower())

if __name__ == '__main__':
    unittest.main()
