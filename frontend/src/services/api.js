const API_BASE = '/api';
const ML_BASE = '/ml';

export const getToken = () => localStorage.getItem('queueless_token');
export const setToken = (token) => localStorage.setItem('queueless_token', token);
export const removeToken = () => {
  localStorage.removeItem('queueless_token');
  localStorage.removeItem('queueless_user');
};

export const getStoredUser = () => {
  const data = localStorage.getItem('queueless_user');
  try {
    return data ? JSON.parse(data) : null;
  } catch (e) {
    return null;
  }
};

export const setStoredUser = (user) => {
  localStorage.setItem('queueless_user', JSON.stringify(user));
};

async function apiRequest(endpoint, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  const contentType = response.headers.get('content-type') || '';
  let data = null;
  if (contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const errorMsg = data?.message || data?.error || (typeof data === 'string' ? data : 'Request failed');
    throw new Error(errorMsg);
  }

  return data;
}

export async function login(email, password) {
  const res = await apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  if (res.accessToken) {
    setToken(res.accessToken);
    const profile = await getMe();
    setStoredUser(profile);
    return { token: res.accessToken, user: profile };
  }
  return res;
}

export async function register({ name, email, password, role }) {
  const res = await apiRequest('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ name, email, password, role }),
  });
  return res;
}

export async function getMe() {
  return await apiRequest('/auth/me');
}

export async function createBusiness(data) {
  return await apiRequest('/businesses', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function getBusiness(id) {
  return await apiRequest(`/businesses/${id}`);
}

export async function createService(businessId, data) {
  return await apiRequest(`/businesses/${businessId}/services`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function getServices(businessId) {
  return await apiRequest(`/businesses/${businessId}/services`);
}

export async function openQueue(serviceId, activeCounters = 2) {
  return await apiRequest(`/services/${serviceId}/queues`, {
    method: 'POST',
    body: JSON.stringify({ activeCounters }),
  });
}

export async function getQueue(id) {
  return await apiRequest(`/queues/${id}`);
}

export async function closeQueue(id) {
  return await apiRequest(`/queues/${id}/close`, {
    method: 'PUT',
  });
}

export async function joinQueue(queueId) {
  return await apiRequest(`/queues/${queueId}/entries`, {
    method: 'POST',
  });
}

export async function getMyPosition(queueId) {
  return await apiRequest(`/queues/${queueId}/entries/my`);
}

export async function cancelMyEntry(entryId) {
  return await apiRequest(`/queue-entries/${entryId}/cancel`, {
    method: 'PUT',
  });
}

export async function getWaitEstimate(entryId) {
  return await apiRequest(`/queue-entries/${entryId}/estimate`);
}

export async function getQueueEntries(queueId) {
  return await apiRequest(`/queues/${queueId}/entries`);
}

export async function callNext(queueId) {
  return await apiRequest(`/queues/${queueId}/entries/call-next`, {
    method: 'PUT',
  });
}

export async function startService(entryId) {
  return await apiRequest(`/queue-entries/${entryId}/start-service`, {
    method: 'PUT',
  });
}

export async function completeService(entryId) {
  return await apiRequest(`/queue-entries/${entryId}/complete`, {
    method: 'PUT',
  });
}

export async function skipEntry(entryId) {
  return await apiRequest(`/queue-entries/${entryId}/skip`, {
    method: 'PUT',
  });
}

export async function getMLHealth() {
  try {
    const res = await fetch(`${ML_BASE}/health`);
    return await res.json();
  } catch (e) {
    return { status: 'OFFLINE', error: e.message };
  }
}

export async function getMLModelInfo() {
  try {
    const res = await fetch(`${ML_BASE}/model-info`);
    return await res.json();
  } catch (e) {
    return null;
  }
}

export async function predictMLDirect(payload) {
  const res = await fetch(`${ML_BASE}/predict`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error('ML Prediction failed');
  return await res.json();
}
