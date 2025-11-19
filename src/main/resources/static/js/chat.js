// WebSocket Connection
function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
    }, function(error) {
        console.log('WebSocket connection error: ' + error);
        // Fallback to polling if WebSocket fails
        setTimeout(connectWebSocket, 5000);
    });
}

// Disconnect WebSocket
function disconnectWebSocket() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
}

// Add this RIGHT HERE
function scrollChatToBottom() {
    const container = document.getElementById('messagesContainer');
    if (container) {
        container.scrollTop = container.scrollHeight;
    }
}


// Theme Toggle
document.addEventListener('DOMContentLoaded', function() {
    // Connect WebSocket on page load
    connectWebSocket();
    
    const themeToggle = document.getElementById('themeToggle');
    const currentTheme = localStorage.getItem('theme') || 'light';
    
    if (currentTheme === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        themeToggle.querySelector('i').classList.remove('fa-moon');
        themeToggle.querySelector('i').classList.add('fa-sun');
    }
    
    themeToggle.addEventListener('click', function() {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        if (currentTheme === 'dark') {
            document.documentElement.setAttribute('data-theme', 'light');
            localStorage.setItem('theme', 'light');
            themeToggle.querySelector('i').classList.remove('fa-sun');
            themeToggle.querySelector('i').classList.add('fa-moon');
        } else {
            document.documentElement.setAttribute('data-theme', 'dark');
            localStorage.setItem('theme', 'dark');
            themeToggle.querySelector('i').classList.remove('fa-moon');
            themeToggle.querySelector('i').classList.add('fa-sun');
        }
    });
    
    // Disconnect on page unload
    window.addEventListener('beforeunload', disconnectWebSocket);
});

// Add Chat Modal
const addChatBtn = document.getElementById('addChatBtn');
const addChatModal = document.getElementById('addChatModal');
const closeModal = document.querySelector('.close');

if (addChatBtn) {
    addChatBtn.addEventListener('click', function() {
        addChatModal.classList.add('active');
    });
}

if (closeModal) {
    closeModal.addEventListener('click', function() {
        addChatModal.classList.remove('active');
    });
}

window.addEventListener('click', function(event) {
    if (event.target === addChatModal) {
        addChatModal.classList.remove('active');
    }
});

function showTab(tabName) {
    const tabs = document.querySelectorAll('.tab-content');
    const tabBtns = document.querySelectorAll('.tab-btn');
    
    tabs.forEach(tab => tab.classList.remove('active'));
    tabBtns.forEach(btn => btn.classList.remove('active'));
    
    document.getElementById(tabName + 'Tab').classList.add('active');
    event.target.classList.add('active');
}

// Private Chat Form
const privateChatForm = document.getElementById('privateChatForm');
if (privateChatForm) {
    privateChatForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        const phoneNumber = document.getElementById('privatePhone').value;
        
        try {
            const response = await fetch('/chat/create-private', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `phoneNumber=${encodeURIComponent(phoneNumber)}`
            });
            
            const result = await response.text();
            if (response.ok) {
                alert('Request sent successfully!');
                addChatModal.classList.remove('active');
                document.getElementById("privateChatsList").innerHTML += `
                    <div class="chat-item" onclick="loadChat(${chatIdFromServer})">
                        ${usernameFromServer}
                    </div>
                `;
            } else {
                alert('Error: ' + result);
            }
        } catch (error) {
            alert('Error: ' + error.message);
        }
    });
}

// Group Chat Form
const groupChatForm = document.getElementById('groupChatForm');
if (groupChatForm) {
    groupChatForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        const groupName = document.getElementById('groupName').value;
        const phoneNumbers = document.getElementById('groupPhones').value;
        
        try {
            const response = await fetch('/chat/create-group', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `groupName=${encodeURIComponent(groupName)}&phoneNumbers=${encodeURIComponent(phoneNumbers)}`
            });
            
            const result = await response.text();
            if (response.ok) {
                alert('Group created and requests sent!');
                addChatModal.classList.remove('active');
                document.getElementById("groupChatsList").innerHTML += `
                    <div class="chat-item" onclick="loadChat(${groupIdFromServer})">
                        ${groupNameFromServer}
                    </div>
                `;
            } else {
                alert('Error: ' + result);
            }
        } catch (error) {
            alert('Error: ' + error.message);
        }
    });
}

// Accept Request
async function acceptRequest(requestId, type) {
    try {
        const response = await fetch(`/chat/accept-request/${requestId}?type=${type}`, {
            method: 'POST'
        });
        
        const result = await response.text();
        if (response.ok) {
            alert('Request accepted!');
            location.reload();
        } else {
            alert('Error: ' + result);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Reject Request
async function rejectRequest(requestId) {
    try {
        const response = await fetch(`/chat/reject-request/${requestId}`, {
            method: 'POST'
        });
        
        const result = await response.text();
        if (response.ok) {
            alert('Request rejected!');
            location.reload();
        } else {
            alert('Error: ' + result);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Load Chat
async function loadChat(chatId) {
    currentChatId = chatId;
    
    // Unsubscribe from previous chat if any
    if (stompClient && stompClient.connected) {
        // Disconnect previous subscription
        disconnectWebSocket();
        connectWebSocket();
    }
    
    try {
        const response = await fetch(`/chat/messages/${chatId}`);
        const messages = await response.json();
        
        const chatContent = document.getElementById('chatContent');
        chatContent.innerHTML = `
            <div class="chat-header">Chat Messages</div>
            <div class="messages-container" id="messagesContainer"></div>
            <div class="message-input-container">
                <button class="btn-emoji" onclick="showEmojiPicker()" title="Emoji">
                    <i class="fas fa-smile"></i>
                </button>
                <input type="text" id="messageInput" placeholder="Type a message..." onkeypress="handleKeyPress(event)">
                <input type="file" id="fileInput" style="display: none;" onchange="handleFileSelect(event)">
                <button class="btn-file" onclick="document.getElementById('fileInput').click()" title="Attach File">
                    <i class="fas fa-paperclip"></i>
                </button>
                <button class="btn-send" onclick="sendMessage()" title="Send">
                    <i class="fas fa-paper-plane"></i>
                </button>
            </div>
        `;
        
        const messagesContainer = document.getElementById('messagesContainer');
        messages.forEach(message => {
            displayMessage(message);
        });

        scrollChatToBottom();

        // Subscribe to WebSocket for this chat
        if (stompClient && stompClient.connected) {
            stompClient.subscribe('/topic/chat/' + chatId, function(message) {
                const messageData = JSON.parse(message.body);
                displayMessage(messageData);
            });
        } else {
            // Fallback to polling if WebSocket not connected
            setInterval(refreshMessages, 2000);
        }
    } catch (error) {
        alert('Error loading chat: ' + error.message);
    }
}

function displayMessage(message) {
    const messagesContainer = document.getElementById('messagesContainer');
    if (!messagesContainer) return;
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${message.sender.id === currentUserId ? 'sent' : 'received'}`;
    
    let content = '';
    if (message.messageType === 'IMAGE') {
        content = `<img src="/${message.filePath}" alt="Image" class="message-image" onclick="window.open('/${message.filePath}', '_blank')">`;
    } else if (message.messageType === 'FILE') {
        const fileName = message.filePath.split('/').pop();
        content = `<div class="message-file">
            <i class="fas fa-file"></i> 
            <a href="/${message.filePath}" download>${fileName}</a>
        </div>`;
    } else {
        content = `<div class="message-content">${escapeHtml(message.content || '')}</div>`;
    }
    
    messageDiv.innerHTML = `
        <div class="message-info">${message.sender.username} - ${formatTime(message.timestamp)}</div>
        ${content}
    `;
    
    messagesContainer.appendChild(messageDiv);
    scrollChatToBottom();

}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatTime(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

async function sendMessage() {
    if (!currentChatId) return;
    
    const messageInput = document.getElementById('messageInput');
    const content = messageInput.value.trim();
    
    if (!content) return;
    
    try {
        // Try WebSocket first
        if (stompClient && stompClient.connected) {
            const messageData = {
                senderId: currentUserId,
                username: currentUsername,
                content: content,
                messageType: 'TEXT',
                filePath: null
            };
            
            stompClient.send('/app/chat/' + currentChatId, {}, JSON.stringify(messageData));
            messageInput.value = '';
        } else {
            // Fallback to HTTP
            const formData = new FormData();
            formData.append('chatId', currentChatId);
            formData.append('content', content);
            
            const response = await fetch('/chat/send-message', {
                method: 'POST',
                body: formData
            });
            
            if (response.ok) {
                messageInput.value = '';
                refreshMessages();
            } else {
                const error = await response.text();
                alert('Error: ' + error);
            }
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function refreshMessages() {
    if (!currentChatId) return;
    
    try {
        const response = await fetch(`/chat/messages/${currentChatId}`);
        const messages = await response.json();
        
        const messagesContainer = document.getElementById('messagesContainer');
        if (!messagesContainer) return;
        
        const currentScroll = messagesContainer.scrollTop;
        const isAtBottom = messagesContainer.scrollHeight - messagesContainer.scrollTop <= messagesContainer.clientHeight + 100;
        
        messagesContainer.innerHTML = '';
        messages.forEach(message => {
            displayMessage(message);
        });
        
        if (isAtBottom) {
            scrollChatToBottom();
        } else {
            messagesContainer.scrollTop = currentScroll;
        }
    } catch (error) {
        console.error('Error refreshing messages:', error);
    }
}

function handleKeyPress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

function handleFileSelect(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (!currentChatId) {
        alert('Please select a chat first');
        return;
    }
    
    const formData = new FormData();
    formData.append('chatId', currentChatId);
    formData.append('file', file);
    
    fetch('/chat/send-message', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (response.ok) {
            refreshMessages();
        } else {
            return response.text().then(text => { throw new Error(text); });
        }
    })
    .catch(error => {
        alert('Error uploading file: ' + error.message);
    });
    
    event.target.value = '';
}

function showEmojiPicker() {
    const emojis = ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', 
                    '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚',
                    '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩'];
    
    const emojiPicker = document.createElement('div');
    emojiPicker.style.cssText = 'position: absolute; bottom: 60px; background: white; border: 1px solid #ddd; border-radius: 10px; padding: 10px; display: grid; grid-template-columns: repeat(6, 1fr); gap: 5px; z-index: 1000;';
    
    emojis.forEach(emoji => {
        const emojiBtn = document.createElement('button');
        emojiBtn.textContent = emoji;
        emojiBtn.style.cssText = 'font-size: 20px; border: none; background: none; cursor: pointer; padding: 5px;';
        emojiBtn.onclick = function() {
            const messageInput = document.getElementById('messageInput');
            if (messageInput) {
                messageInput.value += emoji;
                messageInput.focus();
            }
            emojiPicker.remove();
        };
        emojiPicker.appendChild(emojiBtn);
    });
    
    document.body.appendChild(emojiPicker);
    
    setTimeout(() => {
        if (emojiPicker.parentNode) {
            emojiPicker.remove();
        }
    }, 10000);
}

function goToHomeScreen() {
    currentChatId = null;

    const chatContent = document.getElementById('chatContent');

    chatContent.innerHTML = `
        <div class="welcome-message">
            <h2>Welcome to ChatPulse!</h2>
            <p>Select a chat from the sidebar to start messaging</p>
        </div>
    `;
}


