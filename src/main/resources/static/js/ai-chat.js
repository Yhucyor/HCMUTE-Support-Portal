/**
 * AI Chat Client with SSE Streaming (Phase 3)
 * HCMUTE Student Portal - QAUTE Chatbot
 */

// Global state
let sessionId = currentSessionId || generateUUID();
let eventSource = null;
let currentStreamingMessage = null;

// DOM Elements
const chatMessages = document.getElementById('chatMessages');
const messageInput = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');
const chatForm = document.getElementById('chatForm');
const citationDrawer = document.getElementById('citationDrawer');
const citationContent = document.getElementById('citationContent');
const closeDrawer = document.getElementById('closeDrawer');
const newChatBtn = document.getElementById('newChatBtn');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    // Setup event listeners
    chatForm.addEventListener('submit', handleSendMessage);
    closeDrawer?.addEventListener('click', () => citationDrawer.classList.remove('open'));
    newChatBtn?.addEventListener('click', startNewChat);
    
    // Conversation item clicks
    document.querySelectorAll('.conversation-item').forEach(item => {
        item.addEventListener('click', (e) => {
            if (!e.target.closest('.delete-conv')) {
                const sessionId = item.dataset.sessionId;
                window.location.href = `/ai/chat/${sessionId}`;
            }
        });
    });
    
    // Delete conversation buttons
    document.querySelectorAll('.delete-conv').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const sessionId = btn.dataset.sessionId;
            deleteConversation(sessionId);
        });
    });
    
    // Auto-scroll to bottom on load
    scrollToBottom();
    
    // Focus input
    messageInput.focus();
});

/**
 * Handle send message form submit
 */
async function handleSendMessage(e) {
    e.preventDefault();
    
    const message = messageInput.value.trim();
    if (!message) return;
    
    // Disable input while processing
    setInputState(false);
    
    // Add user message to chat
    addUserMessage(message);
    
    // Clear input
    messageInput.value = '';
    
    // Show typing indicator
    const typingIndicator = showTypingIndicator();
    
    // Send via SSE streaming
    await streamChatResponse(message, typingIndicator);
}

/**
 * Stream chat response using SSE (Server-Sent Events)
 */
async function streamChatResponse(message, typingIndicator) {
    try {
        // Close previous EventSource if exists
        if (eventSource) {
            eventSource.close();
        }
        
        // Prepare request
        const requestBody = {
            query: message,
            sessionId: sessionId,
            provider: 'default'
        };
        
        // Create POST request to start SSE stream
        const response = await fetch('/api/ai/chat-stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        // Remove typing indicator
        typingIndicator.remove();
        
        // Create assistant message container
        currentStreamingMessage = createAssistantMessageContainer();
        
        // Read SSE stream
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        
        while (true) {
            const { done, value } = await reader.read();
            
            if (done) break;
            
            // Decode chunk
            buffer += decoder.decode(value, { stream: true });
            
            // Process SSE events
            const lines = buffer.split('\n');
            buffer = lines.pop() || ''; // Keep incomplete line in buffer
            
            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const data = line.substring(5).trim();
                    
                    if (!data) continue;
                    
                    try {
                        // Extract event type from previous line
                        const eventMatch = lines[lines.indexOf(line) - 1]?.match(/event:\s*(\w+)/);
                        const eventType = eventMatch ? eventMatch[1] : 'message';
                        
                        handleSSEEvent(eventType, data);
                    } catch (err) {
                        console.warn('Failed to parse SSE data:', data, err);
                    }
                }
            }
        }
        
    } catch (error) {
        console.error('Streaming error:', error);
        typingIndicator.remove();
        showErrorMessage('Có lỗi xảy ra khi kết nối với AI. Vui lòng thử lại.');
    } finally {
        setInputState(true);
        currentStreamingMessage = null;
    }
}

/**
 * Handle SSE event based on type
 */
function handleSSEEvent(eventType, data) {
    const parsedData = JSON.parse(data);
    
    switch (eventType) {
        case 'thinking':
            // Already showing typing indicator
            break;
            
        case 'chunk':
            // Append text chunk to streaming message
            appendToStreamingMessage(parsedData.content);
            break;
            
        case 'metadata':
            // Add metadata (sources, confidence, suggestions)
            addMetadataToMessage(parsedData);
            break;
            
        case 'done':
            // Stream completed
            console.log('Stream completed');
            finalizeStreamingMessage();
            break;
            
        case 'error':
            // Handle error
            showErrorMessage(parsedData.error || 'Có lỗi xảy ra');
            if (currentStreamingMessage) {
                currentStreamingMessage.remove();
            }
            break;
            
        default:
            console.warn('Unknown SSE event type:', eventType);
    }
}

/**
 * Add user message bubble to chat
 */
function addUserMessage(text) {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message user-message';
    
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = text;
    
    const timeDiv = document.createElement('div');
    timeDiv.className = 'message-time';
    timeDiv.textContent = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    
    messageDiv.appendChild(contentDiv);
    messageDiv.appendChild(timeDiv);
    chatMessages.appendChild(messageDiv);
    
    scrollToBottom();
}

/**
 * Create assistant message container for streaming
 */
function createAssistantMessageContainer() {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message assistant-message';
    
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.innerHTML = ''; // Will be filled by streaming chunks
    
    const timeDiv = document.createElement('div');
    timeDiv.className = 'message-time';
    timeDiv.textContent = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    
    const metadataDiv = document.createElement('div');
    metadataDiv.className = 'metadata-badges';
    
    messageDiv.appendChild(contentDiv);
    messageDiv.appendChild(metadataDiv);
    messageDiv.appendChild(timeDiv);
    chatMessages.appendChild(messageDiv);
    
    scrollToBottom();
    
    return messageDiv;
}

/**
 * Append text chunk to streaming message (with markdown rendering)
 */
function appendToStreamingMessage(chunk) {
    if (!currentStreamingMessage) return;
    
    const contentDiv = currentStreamingMessage.querySelector('.message-content');
    
    // Append plain text (will render markdown at the end)
    if (!contentDiv.dataset.fullText) {
        contentDiv.dataset.fullText = '';
    }
    contentDiv.dataset.fullText += chunk;
    
    // Render markdown progressively
    contentDiv.innerHTML = marked.parse(contentDiv.dataset.fullText);
    
    scrollToBottom();
}

/**
 * Add metadata to streaming message
 */
function addMetadataToMessage(metadata) {
    if (!currentStreamingMessage) return;
    
    const metadataDiv = currentStreamingMessage.querySelector('.metadata-badges');
    
    // Confidence badge
    if (metadata.confidence) {
        const confidenceBadge = document.createElement('span');
        const confidencePercent = Math.round(metadata.confidence * 100);
        confidenceBadge.className = `badge ${confidencePercent >= 80 ? 'bg-success' : confidencePercent >= 60 ? 'bg-warning' : 'bg-secondary'}`;
        confidenceBadge.innerHTML = `<i class="bi bi-bar-chart-fill"></i> Độ tin cậy: ${confidencePercent}%`;
        metadataDiv.appendChild(confidenceBadge);
    }
    
    // Sources badge (clickable)
    if (metadata.sources && metadata.sources.length > 0) {
        const sourcesBadge = document.createElement('button');
        sourcesBadge.className = 'badge bg-primary';
        sourcesBadge.style.cursor = 'pointer';
        sourcesBadge.innerHTML = `<i class="bi bi-link-45deg"></i> ${metadata.sources.length} nguồn tham khảo`;
        sourcesBadge.addEventListener('click', () => showSources(metadata.sources));
        metadataDiv.appendChild(sourcesBadge);
    }
    
    // Response time
    if (metadata.responseTimeMs) {
        const timeBadge = document.createElement('span');
        timeBadge.className = 'badge bg-secondary';
        timeBadge.innerHTML = `<i class="bi bi-clock"></i> ${(metadata.responseTimeMs / 1000).toFixed(1)}s`;
        metadataDiv.appendChild(timeBadge);
    }
    
    // Update sessionId if provided
    if (metadata.sessionId) {
        sessionId = metadata.sessionId;
    }
}

/**
 * Finalize streaming message (cleanup)
 */
function finalizeStreamingMessage() {
    if (!currentStreamingMessage) return;
    
    const contentDiv = currentStreamingMessage.querySelector('.message-content');
    
    // Final markdown render
    if (contentDiv.dataset.fullText) {
        contentDiv.innerHTML = marked.parse(contentDiv.dataset.fullText);
        delete contentDiv.dataset.fullText;
    }
}

/**
 * Show typing indicator
 */
function showTypingIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'message assistant-message';
    indicator.id = 'typingIndicator';
    
    const typingDiv = document.createElement('div');
    typingDiv.className = 'typing-indicator';
    typingDiv.innerHTML = '<span></span><span></span><span></span>';
    
    indicator.appendChild(typingDiv);
    chatMessages.appendChild(indicator);
    
    scrollToBottom();
    
    return indicator;
}

/**
 * Show error message
 */
function showErrorMessage(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'alert alert-danger alert-dismissible fade show';
    errorDiv.role = 'alert';
    errorDiv.innerHTML = `
        <i class="bi bi-exclamation-triangle-fill"></i> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    chatMessages.appendChild(errorDiv);
    scrollToBottom();
}

/**
 * Show sources in citation drawer
 */
function showSources(sources) {
    citationContent.innerHTML = '';
    
    sources.forEach((source, index) => {
        const sourceCard = document.createElement('div');
        sourceCard.className = 'source-card';
        sourceCard.innerHTML = `
            <div class="source-title">${index + 1}. ${source.title || 'Tài liệu tham khảo'}</div>
            <div class="source-snippet">${source.content || source.snippet || ''}</div>
            <div class="source-meta">
                <span><i class="bi bi-journal-text"></i> ${source.source || 'HCMUTE'}</span>
                ${source.score ? `<span><i class="bi bi-star-fill"></i> ${Math.round(source.score * 100)}%</span>` : ''}
            </div>
        `;
        citationContent.appendChild(sourceCard);
    });
    
    citationDrawer.classList.add('open');
}

/**
 * Set input state (enabled/disabled)
 */
function setInputState(enabled) {
    messageInput.disabled = !enabled;
    sendBtn.disabled = !enabled;
}

/**
 * Scroll chat to bottom
 */
function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

/**
 * Start new chat
 */
function startNewChat() {
    sessionId = generateUUID();
    window.location.href = '/ai/chat';
}

/**
 * Delete conversation
 */
async function deleteConversation(sessionId) {
    if (!confirm('Bạn có chắc muốn xóa hội thoại này?')) return;
    
    try {
        const response = await fetch(`/api/ai/conversations/${sessionId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            // Reload page
            window.location.reload();
        } else {
            alert('Không thể xóa hội thoại');
        }
    } catch (error) {
        console.error('Delete error:', error);
        alert('Có lỗi xảy ra');
    }
}

/**
 * Generate UUID for session ID
 */
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Configure marked.js options
if (typeof marked !== 'undefined') {
    marked.setOptions({
        breaks: true,
        gfm: true,
        headerIds: false,
        mangle: false
    });
}
