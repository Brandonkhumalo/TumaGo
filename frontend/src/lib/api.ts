const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:80/api/v1";

async function fetchAPI(
  endpoint: string,
  options?: RequestInit & { token?: string }
) {
  const { token, ...fetchOptions } = options || {};
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...fetchOptions.headers,
  };

  const res = await fetch(`${API_URL}${endpoint}`, {
    ...fetchOptions,
    headers,
  });

  if (!res.ok) {
    const error = await res
      .json()
      .catch(() => ({ detail: "Request failed" }));
    throw new Error(error.detail || `HTTP ${res.status}`);
  }

  return res.json();
}

export const adminAPI = {
  login: (email: string, password: string) =>
    fetchAPI("/admin/login/", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),
  overview: (token: string) => fetchAPI("/admin/overview/", { token }),
  deliveryMetrics: (token: string, params?: string) =>
    fetchAPI(
      `/admin/deliveries/metrics/${params ? "?" + params : ""}`,
      { token }
    ),
  financialMetrics: (token: string, params?: string) =>
    fetchAPI(
      `/admin/financial/metrics/${params ? "?" + params : ""}`,
      { token }
    ),
  driverMetrics: (token: string) =>
    fetchAPI("/admin/drivers/metrics/", { token }),
  userMetrics: (token: string) =>
    fetchAPI("/admin/users/metrics/", { token }),
  partnerMetrics: (token: string) =>
    fetchAPI("/admin/partners/metrics/", { token }),
  systemHealth: (token: string) =>
    fetchAPI("/admin/system/health/", { token }),
  usersList: (token: string, params?: string) =>
    fetchAPI(`/admin/users/list/${params ? "?" + params : ""}`, {
      token,
    }),
  deliveriesList: (token: string, params?: string) =>
    fetchAPI(`/admin/deliveries/list/${params ? "?" + params : ""}`, {
      token,
    }),
  paymentsList: (token: string, params?: string) =>
    fetchAPI(`/admin/payments/list/${params ? "?" + params : ""}`, {
      token,
    }),

  // Partner management
  createPartner: (token: string, data: Record<string, unknown>) =>
    fetchAPI('/admin/partners/create/', { method: 'POST', token, body: JSON.stringify(data) }),
  togglePartner: (token: string, partnerId: string) =>
    fetchAPI(`/admin/partners/${partnerId}/toggle/`, { method: 'POST', token }),
  partnerDeposit: (token: string, partnerId: string, data: { amount: number; description?: string }) =>
    fetchAPI(`/admin/partners/${partnerId}/deposit/`, { method: 'POST', token, body: JSON.stringify(data) }),
  partnerTransactions: (token: string, partnerId: string, params?: string) =>
    fetchAPI(`/admin/partners/${partnerId}/transactions/${params ? '?' + params : ''}`, { token }),
  editPartner: (token: string, partnerId: string, data: Record<string, unknown>) =>
    fetchAPI(`/admin/partners/${partnerId}/edit/`, { method: 'PUT', token, body: JSON.stringify(data) }),
  markPartnerPaid: (token: string, partnerId: string, paid: boolean) =>
    fetchAPI(`/admin/partners/${partnerId}/mark-paid/`, { method: 'POST', token, body: JSON.stringify({ paid }) }),
  suspendPartner: (token: string, partnerId: string, data: { action: string; duration?: string; reason?: string }) =>
    fetchAPI(`/admin/partners/${partnerId}/suspend/`, { method: 'POST', token, body: JSON.stringify(data) }),
  deletePartner: (token: string, partnerId: string) =>
    fetchAPI(`/admin/partners/${partnerId}/delete/`, { method: 'DELETE', token }),

  // User management
  userDetail: (token: string, userId: string) =>
    fetchAPI(`/admin/users/${userId}/detail/`, { token }),
  banUser: (token: string, userId: string, data: { reason: string; duration: string; custom_until?: string }) =>
    fetchAPI(`/admin/users/${userId}/ban/`, { method: 'POST', token, body: JSON.stringify(data) }),
  unbanUser: (token: string, userId: string) =>
    fetchAPI(`/admin/users/${userId}/unban/`, { method: 'POST', token }),

  // Terms & Conditions
  getTerms: (token: string) =>
    fetchAPI('/admin/terms/', { token }),
  updateTerms: (token: string, data: { app_type: string; content: string }) =>
    fetchAPI('/admin/terms/update/', { method: 'POST', token, body: JSON.stringify(data) }),
};

// ---------------------------------------------------------------------------
// Partner Portal API
// ---------------------------------------------------------------------------

export const partnerAPI = {
  register: (data: { name: string; email: string; password: string; phone: string }) =>
    fetchAPI("/partner/portal/register/", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  paySetup: (data: {
    partner_id: string;
    email: string;
    payment_method: string;
    phone?: string;
  }) =>
    fetchAPI("/partner/portal/pay-setup/", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  paySetupStatus: (partnerId: string, email: string) =>
    fetchAPI(
      `/partner/portal/pay-setup/status/?partner_id=${partnerId}&email=${encodeURIComponent(email)}`
    ),

  login: (email: string, password: string) =>
    fetchAPI("/partner/portal/login/", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),

  account: (token: string) =>
    fetchAPI("/partner/portal/account/", { token }),

  createDevice: (
    token: string,
    data: { label: string; email: string; password: string }
  ) =>
    fetchAPI("/partner/portal/devices/", {
      method: "POST",
      token,
      body: JSON.stringify(data),
    }),

  listDevices: (token: string) =>
    fetchAPI("/partner/portal/devices/list/", { token }),

  toggleDevice: (token: string, deviceId: string) =>
    fetchAPI(`/partner/portal/devices/${deviceId}/toggle/`, {
      method: "POST",
      token,
    }),

  purchaseDeviceSlots: (token: string) =>
    fetchAPI("/partner/portal/devices/purchase/", {
      method: "POST",
      token,
    }),
};

export { fetchAPI };
