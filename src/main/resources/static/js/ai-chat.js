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
    if (!message || message.length < 3) {
        alert('Vui lòng nhập câu hỏi (tối thiểu 3 ký tự)');
        return;
    }
    
    // Disable input while processing
    setInputState(false);
    
    // Add user message to chat
    addUserMessage(message);
    
    // Clear input
    messageInput.value = '';
    
    // Show typing indicator
    const typingIndicator = showTypingIndicator();
    
    // Send via regular POST (simpler than SSE for now)
    await sendChatMessage(message, typingIndicator);
}

/**
 * Send chat message via POST /api/ai/chat (non-streaming)
 */
async function sendChatMessage(message, typingIndicator) {
    try {
        const requestBody = {
            query: message,
            sessionId: sessionId,
            provider: 'default'
        };
        
        const response = await fetch('/api/ai/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        // Remove typing indicator
        typingIndicator.remove();
        
        // Add AI response
        addAssistantMessage(data.data);
        
        // Update sessionId if provided
        if (data.data.sessionId) {
            const newSessionId = data.data.sessionId;
            
            // Nếu đây là conversation mới (sessionId thay đổi)
            if (sessionId !== newSessionId) {
                sessionId = newSessionId;
                
                // Redirect to conversation page để load sidebar với conversation mới
                // Delay 1s để user nhìn thấy response trước khi redirect
                setTimeout(() => {
                    window.location.href = `/ai/chat/${sessionId}`;
                }, 1000);
            } else {
                // Update URL without reload
                window.history.pushState({}, '', `/ai/chat/${sessionId}`);
            }
        }
        
    } catch (error) {
        console.error('Chat error:', error);
        typingIndicator.remove();
        showErrorMessage(error.message || 'Có lỗi xảy ra khi kết nối với AI. Vui lòng thử lại.');
    } finally {
        setInputState(true);
    }
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
    messageDiv.className = 'flex items-start justify-end gap-3 pl-8 md:pl-16 message-animate';
    
    messageDiv.innerHTML = `
        <div class="flex flex-col items-end max-w-xl">
            <div class="flex items-center gap-1.5 mb-1">
                <span class="text-xs font-semibold text-text-primary">${isAuthenticated ? 'Bạn' : 'Guest'}</span>
                <span class="text-[11px] text-text-muted">${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
            </div>
            <div class="p-3.5 rounded-2xl rounded-tr-xs bg-primary text-white leading-relaxed text-sm shadow-xs">
                ${escapeHtml(text)}
            </div>
        </div>
        <div class="w-8 h-8 rounded-full bg-primary-subtle text-primary font-bold text-xs flex items-center justify-center flex-shrink-0 shadow-xs border border-primary/30 mt-1">
            ${isAuthenticated ? 'BẠN' : 'GU'}
        </div>
    `;
    
    const container = chatMessages.querySelector('.max-w-\\[860px\\]');
    container.appendChild(messageDiv);
    scrollToBottom();
}

/**
 * Add assistant message bubble to chat
 */
function addAssistantMessage(data) {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'flex items-start gap-3.5 pr-2 message-animate';
    
    // Render markdown content
    const contentHtml = typeof marked !== 'undefined' ? marked.parse(data.answer) : escapeHtml(data.answer);
    
    // Build sources HTML
    let sourcesHtml = '';
    if (data.sources && data.sources.length > 0) {
        sourcesHtml = `
            <div class="mt-3 p-3 bg-surface-subtle rounded-lg border border-border-ui">
                <div class="flex items-center gap-2 mb-2">
                    <span class="material-symbols-outlined text-sm text-text-muted">link</span>
                    <span class="text-xs font-semibold text-text-secondary">Nguồn tham khảo</span>
                </div>
                <div class="space-y-1.5">
                    ${data.sources.map(src => `
                        <div class="text-xs text-text-secondary hover:text-primary cursor-pointer">
                            <span class="material-symbols-outlined text-[14px] align-middle mr-1">article</span>
                            <span>${escapeHtml(src.title || 'Tài liệu')}</span>
                            ${src.score ? `<span class="text-[10px] text-text-muted"> (${Math.round(src.score * 100)}% độ chính xác)</span>` : ''}
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }
    
    messageDiv.innerHTML = `
        <div class="w-8 h-8 rounded-full bg-primary-subtle p-0.5 border border-primary/20 flex items-center justify-center flex-shrink-0 shadow-xs mt-1">
            <img alt="AI" class="w-full h-full object-contain" 
                 src="https://lh3.googleusercontent.com/aida-public/AB6AXuCL3J_BiPwWcjSiuhpfgKHAM8O0Nn8j4-TRLaxsSGhcQj7Q7ql0ve-QCbKgxddJysjtHibPRDZixwA6ut7G-4cvGHmLfBPz--f2b0mHubxbvdI2_pZ7Lz5jvx519QnHtFxx2QV7d6k_YkiWTGgvwIVW0OWXxa2H2jOL2INtl66jcr6sS2aVFmafAwUAUj6FaROHObrvV9bTNeL5HDgpLQsrjYPJwJp_mVftqATTpOMa1RYuYHESGTEVjP_SyiWFTzu7Rg"/>
        </div>
        <div class="flex-1 flex flex-col space-y-4 min-w-0">
            <div class="flex items-center gap-2">
                <span class="font-heading font-bold text-xs text-primary">Trợ lý AI học vụ HCMUTE</span>
                <span class="px-2 py-0.2 rounded-full text-[10px] font-medium bg-primary-subtle text-primary border border-primary/20">Gemini 1.5 Pro</span>
                <span class="text-[11px] text-text-muted">• ${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
            </div>
            <div class="text-sm text-text-primary leading-relaxed prose prose-sm max-w-none">
                ${contentHtml}
            </div>
            ${sourcesHtml}
            
            <!-- Feedback & Actions Bar -->
            <div class="flex items-center gap-3 pt-2">
                <div class="flex items-center gap-1">
                    <button class="feedback-btn w-7 h-7 rounded-md flex items-center justify-center text-text-muted hover:text-success hover:bg-green-50 transition-all" 
                            data-message-id="${data.messageId || ''}"
                            data-feedback="POSITIVE"
                            onclick="submitFeedback(this)"
                            title="Hữu ích">
                        <span class="material-symbols-outlined text-[16px]">thumb_up</span>
                    </button>
                    <button class="feedback-btn w-7 h-7 rounded-md flex items-center justify-center text-text-muted hover:text-error hover:bg-red-50 transition-all" 
                            data-message-id="${data.messageId || ''}"
                            data-feedback="NEGATIVE"
                            onclick="submitFeedback(this)"
                            title="Không hữu ích">
                        <span class="material-symbols-outlined text-[16px]">thumb_down</span>
                    </button>
                </div>
                <div class="h-4 w-px bg-border-ui"></div>
                <button class="copy-btn w-7 h-7 rounded-md flex items-center justify-center text-text-muted hover:text-primary hover:bg-primary-subtle transition-all" 
                        data-content="${escapeHtml(data.answer)}"
                        onclick="copyResponse(this)"
                        title="Sao chép">
                    <span class="material-symbols-outlined text-[16px]">content_copy</span>
                </button>
            </div>
        </div>
    `;
    
    const container = chatMessages.querySelector('.max-w-\\[860px\\]');
    container.appendChild(messageDiv);
    scrollToBottom();
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
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
 * Start new chat (without page reload to preserve sidebar)
 */
function startNewChat() {
    // Generate new session ID
    sessionId = generateUUID();
    
    // Clear chat messages (keep sidebar intact)
    const container = chatMessages.querySelector('.max-w-\\[860px\\]');
    if (container) {
        container.innerHTML = '';
    }
    
    // Update URL without reload
    window.history.pushState({}, '', '/ai/chat');
    
    // Reset input and focus
    messageInput.value = '';
    messageInput.focus();
    
    console.log('New chat started with sessionId:', sessionId);
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
