import axios from 'axios';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sources?: Source[];
}

export interface Source {
  text: string;
  metadata: {
    type?: string;
    interior_style_name?: string;
    interior_style_image_url?: string;
    architecture_type_name?: string;
    architecture_type_image_url?: string;
    project_name?: string;
    project_image_url?: string;
    project_thumbnail_url?: string;
    news_title?: string;
    news_image_url?: string;
    news_thumbnail_url?: string;
    slide_title?: string;
    slide_image_url?: string;
    [key: string]: any;
  };
  score: number;
}

export interface ChatRequest {
  query: string;
  session_id?: string;
}

export interface ChatResponse {
  answer: string;
  sources?: any[];
  session_id: string;
}

export const chatService = {
  async sendMessage(request: ChatRequest): Promise<ChatResponse> {
    try {
      const response = await axios.post<ChatResponse>(
        `${API_URL}/api/chat`,
        request,
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );
      return response.data;
    } catch (error) {
      console.error('Error sending message:', error);
      throw error;
    }
  },

  async healthCheck(): Promise<boolean> {
    try {
      const response = await axios.get(`${API_URL}/health`);
      return response.status === 200;
    } catch (error) {
      console.error('Health check failed:', error);
      return false;
    }
  },
};
