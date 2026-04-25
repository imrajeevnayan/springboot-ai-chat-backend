const state = {
  apiBase: localStorage.getItem('chat.apiBase') || '',
  userId: localStorage.getItem('chat.userId') || 'user-1',
  model: localStorage.getItem('chat.model') || 'openai/gpt-4o-mini',
  conversationId: localStorage.getItem('chat.conversationId') || '',
  conversations: []
};

const elements = {
  apiBase: document.querySelector('#apiBase'),
  userId: document.querySelector('#userId'),
  model: document.querySelector('#model'),
  healthButton: document.querySelector('#healthButton'),
  healthStatus: document.querySelector('#healthStatus'),
  refreshButton: document.querySelector('#refreshButton'),
  newChatButton: document.querySelector('#newChatButton'),
  deleteButton: document.querySelector('#deleteButton'),
  conversationList: document.querySelector('#conversationList'),
  activeTitle: document.querySelector('#activeTitle'),
  messageStream: document.querySelector('#messageStream'),
  chatForm: document.querySelector('#chatForm'),
  promptInput: document.querySelector('#promptInput'),
  charCount: document.querySelector('#charCount'),
  sendButton: document.querySelector('#sendButton'),
  lastStatus: document.querySelector('#lastStatus'),
  lastResponse: document.querySelector('#lastResponse'),
  toast: document.querySelector('#toast')
};

function initialize() {
  elements.apiBase.value = state.apiBase;
  elements.userId.value = state.userId;
  elements.model.value = state.model;

  bindEvents();
  renderMessages([]);
  refreshConversations();
  checkHealth();

  if (window.lucide) {
    window.lucide.createIcons();
  }
}

function bindEvents() {
  elements.apiBase.addEventListener('change', () => {
    state.apiBase = elements.apiBase.value.trim().replace(/\/$/, '');
    localStorage.setItem('chat.apiBase', state.apiBase);
    checkHealth();
  });

  elements.userId.addEventListener('change', () => {
    state.userId = elements.userId.value.trim() || 'user-1';
    localStorage.setItem('chat.userId', state.userId);
    state.conversationId = '';
    localStorage.removeItem('chat.conversationId');
    renderMessages([]);
    refreshConversations();
  });

  elements.model.addEventListener('change', () => {
    state.model = elements.model.value.trim();
    localStorage.setItem('chat.model', state.model);
  });

  elements.healthButton.addEventListener('click', checkHealth);
  elements.refreshButton.addEventListener('click', refreshConversations);
  elements.newChatButton.addEventListener('click', startNewConversation);
  elements.deleteButton.addEventListener('click', deleteActiveConversation);
  elements.chatForm.addEventListener('submit', sendMessage);
  elements.promptInput.addEventListener('input', updateCharacterCount);
}

async function checkHealth() {
  setHealth('Waiting for API', 'pending');
  try {
    const response = await request('/actuator/health');
    setHealth(`API ${response.status || 'UP'}`, 'up');
  } catch (error) {
    setHealth('API unavailable', 'down');
    updateLastResponse('error', error.payload || { message: error.message });
  }
}

async function refreshConversations() {
  if (!state.userId) {
    renderConversations([]);
    return;
  }

  try {
    const response = await request(`/api/v1/users/${encodeURIComponent(state.userId)}/conversations?page=0&size=50`);
    state.conversations = response.items || [];
    renderConversations(state.conversations);

    const activeExists = state.conversations.some((conversation) => conversation.id === state.conversationId);
    if (state.conversationId && activeExists) {
      await loadMessages(state.conversationId);
    } else {
      updateActiveTitle();
    }
  } catch (error) {
    renderConversations([]);
    updateLastResponse('error', error.payload || { message: error.message });
    showToast(error.message);
  }
}

async function loadMessages(conversationId) {
  state.conversationId = conversationId;
  localStorage.setItem('chat.conversationId', conversationId);
  updateActiveTitle();
  renderConversations(state.conversations);

  try {
    const response = await request(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}/messages?userId=${encodeURIComponent(state.userId)}&page=0&size=100`
    );
    renderMessages(response.items || []);
  } catch (error) {
    updateLastResponse('error', error.payload || { message: error.message });
    showToast(error.message);
  }
}

async function sendMessage(event) {
  event.preventDefault();
  const prompt = elements.promptInput.value.trim();

  if (!prompt) {
    return;
  }

  setBusy(true);
  appendMessage({ role: 'user', content: prompt, createdAt: new Date().toISOString() });

  const payload = {
    userId: state.userId,
    prompt
  };

  if (state.conversationId) {
    payload.conversationId = state.conversationId;
  } else {
    payload.title = createTitle(prompt);
  }

  if (state.model) {
    payload.model = state.model;
  }

  try {
    const response = await request('/api/v1/chat/messages', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    state.conversationId = response.conversationId;
    localStorage.setItem('chat.conversationId', state.conversationId);
    elements.promptInput.value = '';
    updateCharacterCount();
    appendMessage({
      role: response.role,
      content: response.content,
      model: response.model,
      createdAt: response.createdAt
    });
    updateLastResponse('ok', response);
    await refreshConversations();
  } catch (error) {
    updateLastResponse('error', error.payload || { message: error.message });
    showToast(error.message);
  } finally {
    setBusy(false);
  }
}

async function deleteActiveConversation() {
  if (!state.conversationId) {
    showToast('No active conversation selected.');
    return;
  }

  setBusy(true);
  try {
    await request(`/api/v1/conversations/${encodeURIComponent(state.conversationId)}?userId=${encodeURIComponent(state.userId)}`, {
      method: 'DELETE'
    });
    startNewConversation();
    await refreshConversations();
    updateLastResponse('ok', { deleted: true });
  } catch (error) {
    updateLastResponse('error', error.payload || { message: error.message });
    showToast(error.message);
  } finally {
    setBusy(false);
  }
}

function startNewConversation() {
  state.conversationId = '';
  localStorage.removeItem('chat.conversationId');
  elements.promptInput.value = '';
  updateCharacterCount();
  renderMessages([]);
  updateActiveTitle();
  renderConversations(state.conversations);
}

async function request(path, options = {}) {
  const base = state.apiBase || '';
  const response = await fetch(`${base}${path}`, options);
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = payload?.message || `Request failed with status ${response.status}`;
    const error = new Error(message);
    error.payload = payload;
    throw error;
  }

  return payload;
}

function renderConversations(conversations) {
  elements.conversationList.replaceChildren();

  if (!conversations.length) {
    const empty = document.createElement('p');
    empty.className = 'conversation-meta';
    empty.textContent = 'No conversations yet.';
    elements.conversationList.append(empty);
    return;
  }

  const fragment = document.createDocumentFragment();
  conversations.forEach((conversation) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `conversation-item${conversation.id === state.conversationId ? ' is-active' : ''}`;
    button.addEventListener('click', () => loadMessages(conversation.id));

    const title = document.createElement('span');
    title.className = 'conversation-title';
    title.textContent = conversation.title || 'Untitled conversation';

    const meta = document.createElement('span');
    meta.className = 'conversation-meta';
    meta.textContent = formatDate(conversation.updatedAt || conversation.createdAt);

    button.append(title, meta);
    fragment.append(button);
  });

  elements.conversationList.append(fragment);
}

function renderMessages(messages) {
  elements.messageStream.replaceChildren();
  messages.forEach(appendMessage);
}

function appendMessage(message) {
  const article = document.createElement('article');
  article.className = `message ${message.role || 'assistant'}`;

  const role = document.createElement('div');
  role.className = 'message-role';
  role.textContent = message.role || 'assistant';

  const body = document.createElement('div');
  body.className = 'message-body';
  body.textContent = message.content || '';

  article.append(role, body);
  elements.messageStream.append(article);
  elements.messageStream.scrollTop = elements.messageStream.scrollHeight;
}

function updateActiveTitle() {
  const active = state.conversations.find((conversation) => conversation.id === state.conversationId);
  elements.activeTitle.textContent = active?.title || 'New conversation';
  elements.deleteButton.disabled = !state.conversationId;
}

function updateCharacterCount() {
  elements.charCount.textContent = String(elements.promptInput.value.length);
}

function updateLastResponse(status, payload) {
  elements.lastStatus.textContent = status;
  elements.lastStatus.className = `status-pill ${status}`;
  elements.lastResponse.textContent = JSON.stringify(payload || {}, null, 2);
}

function setHealth(message, status) {
  elements.healthStatus.className = `status-line is-${status}`;
  elements.healthStatus.querySelector('span:last-child').textContent = message;
}

function setBusy(isBusy) {
  elements.sendButton.disabled = isBusy;
  elements.deleteButton.disabled = isBusy || !state.conversationId;
  elements.promptInput.disabled = isBusy;
}

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.add('show');
  window.setTimeout(() => elements.toast.classList.remove('show'), 3600);
}

function createTitle(prompt) {
  return prompt.length > 56 ? `${prompt.slice(0, 53)}...` : prompt;
}

function formatDate(value) {
  if (!value) {
    return 'No timestamp';
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

initialize();
