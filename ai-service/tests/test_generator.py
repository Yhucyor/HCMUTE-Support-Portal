import unittest
from unittest.mock import Mock, patch
from llm.generator import generate_answer

class TestGenerator(unittest.TestCase):
    
    def test_empty_context(self):
        """Test với context rỗng"""
        result = generate_answer("", "test question")
        self.assertIn("ngữ cảnh", result)
    
    def test_empty_question(self):
        """Test với question rỗng"""
        result = generate_answer("test context", "")
        self.assertIn("Câu hỏi", result)
    
    @patch('llm.generator.ollama.Client')
    @patch('llm.generator.build_prompt')
    def test_successful_generation(self, mock_prompt, mock_client_class):
        """Test generation thành công"""
        mock_prompt.return_value = "Test prompt"
        
        # Mock Ollama response
        mock_response = {
            'message': {
                'content': 'Test answer'
            }
        }
        
        mock_client = Mock()
        mock_client.chat.return_value = mock_response
        mock_client_class.return_value = mock_client
        
        result = generate_answer("test context", "test question")
        self.assertEqual(result, "Test answer")
    
    @patch('llm.generator.ollama.Client')
    @patch('llm.generator.build_prompt')
    def test_ollama_timeout(self, mock_prompt, mock_client_class):
        """Test xử lý timeout"""
        mock_prompt.return_value = "Test prompt"
        
        mock_client = Mock()
        mock_client.chat.side_effect = TimeoutError("Request timeout")
        mock_client_class.return_value = mock_client
        
        result = generate_answer("test context", "test question")
        self.assertIn("quá lâu", result)
    
    @patch('llm.generator.ollama.Client')
    @patch('llm.generator.build_prompt')
    def test_ollama_error(self, mock_prompt, mock_client_class):
        """Test xử lý lỗi Ollama"""
        import ollama
        mock_prompt.return_value = "Test prompt"
        
        mock_client = Mock()
        mock_client.chat.side_effect = ollama.ResponseError("API error")
        mock_client_class.return_value = mock_client
        
        result = generate_answer("test context", "test question")
        self.assertIn("gặp vấn đề", result)

if __name__ == '__main__':
    unittest.main()
