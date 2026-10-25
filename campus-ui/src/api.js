const API_ROOT = "/api";

async function request(path, options) {
  let response;
  try {
    response = await fetch(`${API_ROOT}${path}`, {
      ...options,
      headers: {
        ...(options?.body ? { "Content-Type": "application/json" } : {}),
        ...options?.headers,
      },
    });
  } catch {
    throw new Error("Unable to reach the Campus API. Check that the Java server is running on port 8080.");
  }

  const payload = await response.json().catch(() => null);
  if (!response.ok || payload?.ok === false) {
    const error = new Error(payload?.message || `Request failed with status ${response.status}`);
    error.name = payload?.error || "ApiError";
    error.status = response.status;
    throw error;
  }
  return payload;
}

const post = (path, body = {}) => request(path, {
  method: "POST",
  body: JSON.stringify(body),
});

export const api = {
  resources: () => request("/resources"),
  search: (query) => request(`/search?q=${encodeURIComponent(query)}`),
  bookings: () => request("/bookings"),
  users: () => request("/users"),
  logs: () => request("/logs"),
  book: (userId, resourceId) => post("/book", { userId, resourceId }),
  cancel: (userId, bookingId) => post("/cancel", { userId, bookingId }),
  save: () => post("/save"),
  demoConcurrency: () => post("/demo-concurrency"),
};
