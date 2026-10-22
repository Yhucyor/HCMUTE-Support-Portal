/**
 * AI Chat Client with SSE Streaming (Phase 3)
 * HCMUTE Student Portal - QAUTE Chatbot
 */

// Global state
let sessionId = currentSessionId || generateUUID();
let eventSource = null;
let currentStreamingMessage = null;

// Selected AI model state
let selectedProvider = localStorage.getItem('qaute_selected_provider') || 'gemini';
let selectedModelName = localStorage.getItem('qaute_selected_model_name') || 'Gemini 1.5 Pro';
let selectedModelIcon = localStorage.getItem('qaute_selected_model_icon') || 'auto_awesome';

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
    // Render markdown for server-rendered AI messages
    renderMarkdownMessages();
    
    // Setup event listeners
    chatForm.addEventListener('submit', handleSendMessage);
    closeDrawer?.addEventListener('click', () => citationDrawer.classList.remove('open'));
    newChatBtn?.addEventListener('click', startNewChat);
    
    // Conversation item clicks & delete buttons
    bindConversationItemEvents();
    
    // Suggestion cards
    attachSuggestionListeners();
    
    // Model selector dropdown
    initModelSelector();
    
    // Enter to send, Shift+Enter for newline
    messageInput?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (chatForm.requestSubmit) {
                chatForm.requestSubmit();
            } else {
                chatForm.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
            }
        }
    });
    
    // Auto-scroll to bottom on load
    scrollToBottom();
    
    // Focus input
    messageInput?.focus();
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
            provider: selectedProvider || 'gemini'
        };
        
        const response = await fetch('/api/ai/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',  // 🔥 CRITICAL: Include session cookies for authentication
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
            sessionId = data.data.sessionId;
            
            // Update URL without reload
            window.history.pushState({}, '', `/ai/chat/${sessionId}`);
            
            // Update chat header title & reveal header rename button if was new chat
            const headerTitle = document.getElementById('currentConversationTitle');
            if (headerTitle && (headerTitle.textContent.trim() === 'Trợ lý AI học vụ' || headerTitle.textContent.trim() === 'Cuộc trò chuyện')) {
                const shortTitle = message.length > 40 ? message.substring(0, 37) + '...' : message;
                headerTitle.textContent = shortTitle;
            }
            let headerBtn = document.getElementById('headerRenameBtn');
            if (!headerBtn && headerTitle) {
                headerBtn = document.createElement('button');
                headerBtn.id = 'headerRenameBtn';
                headerBtn.className = 'p-0.5 text-text-muted hover:text-primary transition-colors rounded';
                headerBtn.title = 'Đổi tên cuộc trò chuyện';
                headerBtn.type = 'button';
                headerBtn.innerHTML = '<span class="material-symbols-outlined text-[16px] leading-none">edit</span>';
                headerTitle.after(headerBtn);
            }
            if (headerBtn) {
                headerBtn.dataset.sessionId = sessionId;
                headerBtn.dataset.title = headerTitle ? headerTitle.textContent.trim() : message;
                headerBtn.onclick = function() {
                    if (window.openRenameModal) {
                        window.openRenameModal(this.dataset.sessionId, this.dataset.title);
                    }
                };
            }
            
            // Refresh sidebar immediately so the conversation shows on the left
            refreshSidebarConversations();
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
            provider: selectedProvider || 'gemini'
        };
        
        // Create POST request to start SSE stream
        const response = await fetch('/api/ai/chat-stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',  // Include session cookies
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
    const container = chatMessages.querySelector('.max-w-\\[860px\\]');
    if (!container) return;
    
    // Remove welcome screen if present
    const welcomeScreen = container.querySelector('#welcomeScreen') || container.querySelector('.text-center.mt-20');
    if (welcomeScreen) {
        welcomeScreen.remove();
    }
    
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
                <span class="px-2 py-0.2 rounded-full text-[10px] font-medium bg-primary-subtle text-primary border border-primary/20">${getModelDisplayName(data.provider || selectedProvider)}</span>
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
    const container = chatMessages.querySelector('.max-w-\\[860px\\]') || chatMessages;
    
    const indicator = document.createElement('div');
    indicator.className = 'flex items-start gap-3.5 pr-2 message-animate';
    indicator.id = 'typingIndicator';
    
    const modelName = getModelDisplayName(selectedProvider);
    
    indicator.innerHTML = `
        <div class="w-8 h-8 rounded-full bg-primary-subtle p-0.5 border border-primary/20 flex items-center justify-center flex-shrink-0 shadow-xs mt-1 animate-pulse">
            <img alt="AI" class="w-full h-full object-contain" 
                 src="https://lh3.googleusercontent.com/aida-public/AB6AXuCL3J_BiPwWcjSiuhpfgKHAM8O0Nn8j4-TRLaxsSGhcQj7Q7ql0ve-QCbKgxddJysjtHibPRDZixwA6ut7G-4cvGHmLfBPz--f2b0mHubxbvdI2_pZ7Lz5jvx519QnHtFxx2QV7d6k_YkiWTGgvwIVW0OWXxa2H2jOL2INtl66jcr6sS2aVFmafAwUAUj6FaROHObrvV9bTNeL5HDgpLQsrjYPJwJp_mVftqATTpOMa1RYuYHESGTEVjP_SyiWFTzu7Rg"/>
        </div>
        <div class="flex-1 flex flex-col space-y-2 min-w-0">
            <div class="flex items-center gap-2">
                <span class="font-heading font-bold text-xs text-primary">Trợ lý AI học vụ HCMUTE</span>
                <span class="px-2 py-0.2 rounded-full text-[10px] font-medium bg-primary-subtle text-primary border border-primary/20 flex items-center gap-1">
                    <span class="w-1.5 h-1.5 rounded-full bg-primary animate-ping"></span>
                    ${modelName}
                </span>
            </div>
            <div class="p-3.5 bg-surface-subtle border border-border-ui/80 rounded-2xl rounded-tl-sm shadow-xs inline-flex items-center gap-3 w-fit">
                <div class="flex items-center gap-1.5 px-1">
                    <span class="w-2 h-2 rounded-full bg-primary animate-bounce" style="animation-delay: 0s"></span>
                    <span class="w-2 h-2 rounded-full bg-primary animate-bounce" style="animation-delay: 0.15s"></span>
                    <span class="w-2 h-2 rounded-full bg-primary animate-bounce" style="animation-delay: 0.3s"></span>
                </div>
                <span class="text-xs font-medium text-text-secondary animate-pulse" id="typingIndicatorText">
                    AI đang suy nghĩ câu trả lời...
                </span>
            </div>
        </div>
    `;
    
    container.appendChild(indicator);
    scrollToBottom();
    
    // Luân chuyển thông điệp mô tả trạng thái AI
    let step = 0;
    const texts = [
        "AI đang suy nghĩ câu trả lời...",
        "Đang tra cứu dữ liệu học vụ HCMUTE...",
        "Đang tổng hợp câu trả lời chi tiết..."
    ];
    const intervalId = setInterval(() => {
        step = (step + 1) % texts.length;
        const textElem = indicator.querySelector('#typingIndicatorText');
        if (textElem) {
            textElem.textContent = texts[step];
        }
    }, 2800);
    
    // Ghi đè hàm remove để tự động hủy interval
    const originalRemove = indicator.remove.bind(indicator);
    indicator.remove = function() {
        clearInterval(intervalId);
        originalRemove();
    };
    
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
async function startNewChat() {
    // Generate new session ID
    sessionId = generateUUID();
    
    // Reset chat messages to welcome screen
    const container = chatMessages.querySelector('.max-w-\\[860px\\]');
    if (container) {
        container.innerHTML = getWelcomeScreenHtml();
        attachSuggestionListeners();
    }
    
    // Refresh sidebar so current conversation is saved and listed on the left
    await refreshSidebarConversations();
    
    // Update URL without reload
    window.history.pushState({}, '', '/ai/chat');
    
    // Reset input and focus
    messageInput.value = '';
    messageInput.focus();
    
    console.log('New chat started with sessionId:', sessionId);
}

/**
 * Attach click listeners to suggestion cards
 */
function attachSuggestionListeners() {
    document.querySelectorAll('.suggestion-card').forEach(card => {
        card.addEventListener('click', () => {
            const prompt = card.dataset.prompt;
            if (prompt) {
                messageInput.value = prompt;
                messageInput.focus();
            }
        });
    });
}

/**
 * Get Welcome Screen HTML
 */
function getWelcomeScreenHtml() {
    return `
        <div id="welcomeScreen" class="text-center mt-20">
            <div class="w-16 h-16 mx-auto rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center mb-6 shadow-lg">
                <span class="material-symbols-outlined text-white text-3xl" style="font-variation-settings: 'FILL' 1;">auto_awesome</span>
            </div>
            <h2 class="font-heading text-2xl font-bold text-text-primary mb-3">Chào mừng đến với QAUTE!</h2>
            <p class="text-text-secondary mb-8 max-w-md mx-auto">Tôi là trợ lý AI của HCMUTE. Hãy hỏi tôi về học tập, đào tạo và thủ tục sinh viên.</p>
            
            <!-- Suggestion Cards -->
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 max-w-3xl mx-auto">
                <div class="suggestion-card bg-white rounded-xl p-4 border border-border-ui hover:border-blue-300 hover:shadow-md transition-all cursor-pointer" data-prompt="Hỏi về môn học và điều kiện tốt nghiệp">
                    <span class="material-symbols-outlined text-3xl text-blue-600 mb-2">school</span>
                    <p class="text-sm font-semibold text-text-primary mb-1">Chương trình đào tạo</p>
                    <p class="text-xs text-text-secondary">Hỏi về môn học, điều kiện tốt nghiệp</p>
                </div>
                <div class="suggestion-card bg-white rounded-xl p-4 border border-border-ui hover:border-green-300 hover:shadow-md transition-all cursor-pointer" data-prompt="Xem lịch học, lịch thi, thời khóa biểu">
                    <span class="material-symbols-outlined text-3xl text-green-600 mb-2">calendar_month</span>
                    <p class="text-sm font-semibold text-text-primary mb-1">Lịch học & thi</p>
                    <p class="text-xs text-text-secondary">Xem lịch học, lịch thi, thời khóa biểu</p>
                </div>
                <div class="suggestion-card bg-white rounded-xl p-4 border border-border-ui hover:border-amber-300 hover:shadow-md transition-all cursor-pointer" data-prompt="Hỏi về giấy tờ, đơn từ, quy trình sinh viên">
                    <span class="material-symbols-outlined text-3xl text-amber-600 mb-2">description</span>
                    <p class="text-sm font-semibold text-text-primary mb-1">Thủ tục hành chính</p>
                    <p class="text-xs text-text-secondary">Hỏi về giấy tờ, đơn từ, quy trình</p>
                </div>
            </div>
        </div>
    `;
}

/**
 * Format conversation updatedAt timestamp
 */
function formatConvTime(updatedAt) {
    if (!updatedAt) return 'Vừa xong';
    try {
        let date;
        if (Array.isArray(updatedAt)) {
            date = new Date(updatedAt[0], updatedAt[1] - 1, updatedAt[2], updatedAt[3] || 0, updatedAt[4] || 0);
        } else {
            date = new Date(updatedAt);
        }
        if (isNaN(date.getTime())) return 'Vừa xong';
        return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
        return 'Vừa xong';
    }
}

/**
 * Refresh sidebar conversation list from API
 */
async function refreshSidebarConversations() {
    try {
        const response = await fetch('/api/ai/conversations', {
            credentials: 'include'
        });
        if (!response.ok) return;
        const result = await response.json();
        const conversations = result.data || [];
        renderSidebarConversations(conversations);
    } catch (error) {
        console.error('Error refreshing sidebar conversations:', error);
    }
}

/**
 * Render sidebar conversations dynamically
 */
function renderSidebarConversations(conversations) {
    const historyContainer = document.getElementById('conversationHistoryList');
    if (!historyContainer) return;
    
    if (!conversations || conversations.length === 0) {
        historyContainer.innerHTML = `
            <div class="text-center text-text-muted mt-12">
                <span class="material-symbols-outlined text-5xl text-slate-300">chat_bubble_outline</span>
                <p class="text-sm mt-3">Chưa có hội thoại nào</p>
            </div>
        `;
        return;
    }
    
    historyContainer.innerHTML = `
        <div class="space-y-1">
            <div class="px-2.5 py-1 text-[11px] font-semibold text-text-muted uppercase tracking-wider flex items-center justify-between">
                <span class="flex items-center gap-1.5">
                    <span class="w-1.5 h-1.5 rounded-full bg-primary"></span>Gần đây
                </span>
                <span class="text-[10px] text-text-muted font-mono">${conversations.length}</span>
            </div>
            <div class="space-y-1" id="conversationItemsList">
                ${conversations.map(conv => {
                    const isActive = (conv.sessionId === sessionId);
                    const timeStr = formatConvTime(conv.updatedAt || conv.createdAt);
                    const count = conv.messageCount || 0;
                    return `
                        <div class="group conversation-item relative flex items-center justify-between px-2.5 py-2 rounded-xl cursor-pointer transition-all hover:bg-surface-subtle ${isActive ? 'bg-primary-subtle text-primary font-medium border border-primary/20' : 'text-text-secondary'}"
                             data-session-id="${escapeHtml(conv.sessionId)}">
                            <div class="flex items-center gap-2 min-w-0 pr-1">
                                <span class="material-symbols-outlined text-base flex-shrink-0 ${isActive ? 'text-primary' : 'text-text-muted'}">
                                    chat
                                </span>
                                <div class="flex flex-col min-w-0">
                                    <span class="text-xs font-semibold truncate conv-title-text" title="${escapeHtml(conv.title || 'Hội thoại mới')}">${escapeHtml(conv.title || 'Hội thoại mới')}</span>
                                    <span class="text-[10px] text-text-secondary truncate">
                                        ${timeStr} • ${count} tin nhắn
                                    </span>
                                </div>
                            </div>
                            <div class="relative flex items-center gap-0.5">
                                <button class="opacity-0 group-hover:opacity-100 p-1 text-text-secondary hover:text-primary hover:bg-white/80 rounded transition-opacity rename-conv" 
                                        data-session-id="${escapeHtml(conv.sessionId)}"
                                        data-title="${escapeHtml(conv.title || 'Hội thoại mới')}"
                                        onclick="event.stopPropagation(); window.openRenameModal(this.dataset.sessionId, this.dataset.title)" 
                                        title="Đổi tên" type="button">
                                    <span class="material-symbols-outlined text-[15px] leading-none">edit</span>
                                </button>
                                <button class="opacity-0 group-hover:opacity-100 p-1 text-text-secondary hover:text-error hover:bg-white/80 rounded transition-opacity delete-conv" 
                                        data-session-id="${escapeHtml(conv.sessionId)}"
                                        onclick="event.stopPropagation(); deleteConversation(this.dataset.sessionId)" 
                                        title="Xóa" type="button">
                                    <span class="material-symbols-outlined text-[15px] leading-none">delete</span>
                                </button>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        </div>
    `;
    
    bindConversationItemEvents();
}

/**
 * Bind click and delete events for conversation items
 */
function bindConversationItemEvents() {
    document.querySelectorAll('.conversation-item').forEach(item => {
        item.addEventListener('click', (e) => {
            if (!e.target.closest('.delete-conv') && !e.target.closest('.rename-conv')) {
                const sid = item.dataset.sessionId;
                if (sid) {
                    window.location.href = `/ai/chat/${sid}`;
                }
            }
        });
    });
    
    document.querySelectorAll('.delete-conv').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const sid = btn.dataset.sessionId;
            if (sid) {
                deleteConversation(sid);
            }
        });
    });

    document.querySelectorAll('.rename-conv').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const sid = btn.dataset.sessionId;
            const title = btn.dataset.title || '';
            if (sid && window.openRenameModal) {
                window.openRenameModal(sid, title);
            }
        });
    });
}

/**
 * Delete conversation
 */
async function deleteConversation(sessionIdToDelete) {
    if (!confirm('Bạn có chắc muốn xóa hội thoại này?')) return;
    
    try {
        const response = await fetch(`/api/ai/conversations/${sessionIdToDelete}`, {
            method: 'DELETE',
            credentials: 'include'  // Include session cookies
        });
        
        if (response.ok) {
            if (sessionId === sessionIdToDelete) {
                startNewChat();
            } else {
                refreshSidebarConversations();
            }
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

/**
 * Render markdown for server-rendered AI messages
 */
function renderMarkdownMessages() {
    if (typeof marked === 'undefined') return;
    document.querySelectorAll('.ai-markdown-content').forEach(el => {
        if (el.dataset.parsed === 'true') return;
        const raw = el.textContent || '';
        if (raw.trim()) {
            el.innerHTML = marked.parse(raw);
            el.dataset.parsed = 'true';
        }
    });
}

// Run immediately if DOM already loaded
if (document.readyState === 'complete' || document.readyState === 'interactive') {
    renderMarkdownMessages();
    initModelSelector();
}

/**
 * Map provider key to readable display name
 */
function getModelDisplayName(provider) {
    if (!provider) return 'Gemini 1.5 Pro';
    const p = provider.toLowerCase();
    if (p.includes('gemini')) return 'Gemini 1.5 Pro';
    if (p.includes('ollama') || p.includes('llama')) return 'Llama 3.2 (Ollama)';
    if (p.includes('dify')) return 'Dify AI Agent';
    if (p.includes('faq') || p.includes('knowledge') || p.includes('default')) return 'QAUTE FAQ Hybrid';
    return provider;
}

/**
 * Initialize model selector dropdown
 */
function initModelSelector() {
    const btn = document.getElementById('modelSelectorBtn');
    const menu = document.getElementById('modelDropdownMenu');
    const textEl = document.getElementById('selectedModelText');
    const iconEl = document.getElementById('selectedModelIcon');
    
    if (!btn || !menu) return;
    
    // Set initial display from localStorage or defaults
    if (textEl) textEl.textContent = selectedModelName;
    if (iconEl) iconEl.textContent = selectedModelIcon;
    
    // Update checkmarks
    updateModelCheckmarks();
    
    // Toggle menu
    btn.onclick = function(e) {
        e.stopPropagation();
        menu.classList.toggle('hidden');
        btn.setAttribute('aria-expanded', !menu.classList.contains('hidden'));
    };
    
    // Close on click outside
    document.addEventListener('click', (e) => {
        if (!menu.classList.contains('hidden') && !btn.contains(e.target) && !menu.contains(e.target)) {
            menu.classList.add('hidden');
            btn.setAttribute('aria-expanded', 'false');
        }
    });
    
    // Select option
    document.querySelectorAll('.model-option').forEach(option => {
        option.onclick = function(e) {
            e.stopPropagation();
            selectedProvider = option.dataset.provider || 'gemini';
            selectedModelName = option.dataset.name || 'Gemini 1.5 Pro';
            selectedModelIcon = option.dataset.icon || 'auto_awesome';
            
            localStorage.setItem('qaute_selected_provider', selectedProvider);
            localStorage.setItem('qaute_selected_model_name', selectedModelName);
            localStorage.setItem('qaute_selected_model_icon', selectedModelIcon);
            
            if (textEl) textEl.textContent = selectedModelName;
            if (iconEl) iconEl.textContent = selectedModelIcon;
            
            updateModelCheckmarks();
            menu.classList.add('hidden');
            btn.setAttribute('aria-expanded', 'false');
        };
    });
}

function updateModelCheckmarks() {
    document.querySelectorAll('.model-option').forEach(option => {
        const isCurrent = (option.dataset.provider === selectedProvider);
        const checkIcon = option.querySelector('.check-icon');
        if (checkIcon) {
            checkIcon.classList.toggle('hidden', !isCurrent);
        }
    });
}
