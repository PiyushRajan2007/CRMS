import { useCallback, useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { api } from "./api.js";
import ConcurrencyDemo from "./ConcurrencyDemo.jsx";
import LogsPanel from "./LogsPanel.jsx";
import OopOverlay from "./OopOverlay.jsx";
import ResourceScene from "./ResourceScene.jsx";

function ResourcePanel({ resource, booking, user, onBook, onCancel, onClose, busy }) {
  if (!resource)
    return null;
  return (
    <motion.aside
      className="resource-panel glass"
      initial={{ x: 28, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      exit={{ x: 28, opacity: 0 }}
      transition={{ type: "spring", damping: 25, stiffness: 230 }}
    >
      <button className="icon-button panel-close" onClick={onClose} aria-label="Close resource details">×</button>
      <div className="eyebrow"><span className="live-dot" /> RESOURCE PROFILE</div>
      <div className={`large-resource-icon ${resource.available ? "available" : "booked"}`}>
        {resource.type?.toLowerCase() === "book" ? "▤" : resource.type?.toLowerCase() === "lab" ? "⬡" : "◇"}
      </div>
      <span className="resource-type">{resource.type}</span>
      <h2>{resource.name}</h2>
      <div className="resource-id">ASSET · {resource.id}</div>
      <div className={`availability ${resource.available ? "is-available" : "is-booked"}`}>
        <span />{resource.available ? "AVAILABLE NOW" : "CURRENTLY BOOKED"}
      </div>
      <div className="panel-divider" />
      {booking ? (
        <>
          <p className="panel-hint">This resource is in your bookings as <strong>{booking.id}</strong>.</p>
          <button className="action-button danger" onClick={() => onCancel(booking.id)} disabled={busy}>
            {busy ? "PROCESSING..." : "Cancel booking"}
          </button>
        </>
      ) : resource.available ? (
        <button className="action-button" onClick={() => onBook(resource.id)} disabled={!user || busy}>
          {busy ? "PROCESSING..." : "Book resource"} <span>↗</span>
        </button>
      ) : (
        <p className="panel-hint">This resource is unavailable. Try another spot on the map.</p>
      )}
      {!booking && user && <p className="panel-hint">Booking as <strong>{user.id}</strong> · {user.bookingLimit} booking limit</p>}
    </motion.aside>
  );
}

function BookingList({ bookings, resources, user, onSelect, onCancel, busyId }) {
  const ownBookings = bookings.filter((booking) => booking.userId === user?.id);
  return (
    <section className="bookings-card glass">
      <div className="section-heading">
        <div><div className="eyebrow">YOUR ACTIVITY</div><h3>My bookings</h3></div>
        <span className="count-pill">{ownBookings.filter((item) => item.active).length.toString().padStart(2, "0")}</span>
      </div>
      {!ownBookings.length ? (
        <div className="empty-bookings"><span>⌁</span><p>No bookings yet</p><small>Your reservations will appear here.</small></div>
      ) : (
        <div className="booking-list">
          {ownBookings.map((booking) => {
            const resource = resources.find((item) => item.id === booking.resourceId);
            return (
              <article className={`booking-row ${booking.active ? "" : "is-cancelled"}`} key={booking.id}>
                <button className="booking-main" onClick={() => resource && onSelect(resource)}>
                  <span className={`booking-mark ${booking.active ? "available" : ""}`}>◇</span>
                  <span className="booking-info">
                    <strong>{resource?.name || booking.resourceId}</strong>
                    <small>{booking.id} · {booking.active ? "ACTIVE" : "CANCELLED"}</small>
                  </span>
                </button>
                {booking.active && (
                  <button className="cancel-mini" onClick={() => onCancel(booking.id)} disabled={busyId === booking.id} aria-label={`Cancel ${booking.id}`}>
                    {busyId === booking.id ? "…" : "×"}
                  </button>
                )}
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

function Toasts({ toasts }) {
  return (
    <div className="toast-stack" aria-live="polite">
      <AnimatePresence>
        {toasts.map((toast) => (
          <motion.div
            className={`toast glass ${toast.error ? "toast-error" : "toast-success"}`}
            key={toast.id}
            initial={{ opacity: 0, x: 30, scale: 0.95 }}
            animate={toast.error ? { opacity: 1, x: [0, -7, 7, -4, 0], scale: 1 } : { opacity: 1, x: 0, scale: 1 }}
            exit={{ opacity: 0, x: 24 }}
          >
            <span className="toast-icon">{toast.error ? "!" : "✓"}</span>
            <span><strong>{toast.title}</strong><small>{toast.message}</small></span>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}

export default function App() {
  const [resources, setResources] = useState([]);
  const [users, setUsers] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedResource, setSelectedResource] = useState(null);
  const [query, setQuery] = useState("");
  const [searchMatches, setSearchMatches] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [serverDown, setServerDown] = useState(false);
  const [intro, setIntro] = useState(true);
  const [busy, setBusy] = useState(false);
  const [busyId, setBusyId] = useState("");
  const [activePanel, setActivePanel] = useState("");
  const [saveState, setSaveState] = useState("idle");
  const [toasts, setToasts] = useState([]);

  const selectedUser = users.find((user) => user.id === selectedUserId) || users[0];
  const selectedBooking = useMemo(
    () => bookings.find((booking) => booking.resourceId === selectedResource?.id && booking.userId === selectedUser?.id && booking.active),
    [bookings, selectedResource, selectedUser],
  );

  const refresh = useCallback(async (showLoading = false) => {
    if (showLoading)
      setLoading(true);
    try {
      const [nextResources, nextUsers, nextBookings] = await Promise.all([api.resources(), api.users(), api.bookings()]);
      setResources(nextResources);
      setUsers(nextUsers);
      setBookings(nextBookings);
      setSelectedUserId((current) => nextUsers.some((user) => user.id === current) ? current : nextUsers[0]?.id || "");
      setSelectedResource((current) => current ? nextResources.find((resource) => resource.id === current.id) || current : null);
      setServerDown(false);
    } catch (error) {
      setServerDown(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh(true);
    const timer = window.setInterval(() => refresh(), 12000);
    return () => window.clearInterval(timer);
  }, [refresh]);

  useEffect(() => {
    if (!query.trim()) {
      setSearchMatches(new Set());
      return;
    }
    const timer = window.setTimeout(async () => {
      try {
        const matches = await api.search(query);
        setSearchMatches(new Set(matches.map((resource) => resource.id)));
      } catch (error) {
        pushToast(error);
      }
    }, 250);
    return () => window.clearTimeout(timer);
  }, [query]);

  const pushToast = useCallback((error, successTitle) => {
    const id = `${Date.now()}-${Math.random()}`;
    const isError = error instanceof Error && !successTitle;
    const toast = isError
      ? { id, error: true, title: error.name || "Error", message: error.message }
      : { id, error: false, title: successTitle || "Complete", message: error?.message || "" };
    setToasts((current) => [...current, toast]);
    window.setTimeout(() => setToasts((current) => current.filter((item) => item.id !== id)), 4200);
  }, []);

  async function book(resourceId) {
    if (!selectedUser)
      return;
    setBusy(true);
    try {
      const result = await api.book(selectedUser.id, resourceId);
      pushToast({ message: `Reservation ${result.bookingId} confirmed.` }, "Resource booked");
      await refresh();
    } catch (error) {
      pushToast(error);
    } finally {
      setBusy(false);
    }
  }

  async function cancel(bookingId) {
    setBusyId(bookingId);
    try {
      await api.cancel(bookingId);
      pushToast({ message: `${bookingId} has been cancelled.` }, "Booking cancelled");
      await refresh();
    } catch (error) {
      pushToast(error);
    } finally {
      setBusyId("");
    }
  }

  async function saveData() {
    setSaveState("saving");
    try {
      await api.save();
      setSaveState("saved");
      pushToast({ message: "Campus data saved to the server." }, "Changes saved");
      window.setTimeout(() => setSaveState("idle"), 2200);
    } catch (error) {
      setSaveState("idle");
      pushToast(error);
    }
  }

  function selectResource(resource) {
    setSelectedResource(resource);
  }

  return (
    <main className="app-shell">
      <div className="ambient ambient-one" /><div className="ambient ambient-two" />
      <header className="topbar">
        <a className="brand" href="/" aria-label="Northbridge Campus Atlas home">
          <span className="brand-mark"><i /><i /><i /></span>
          <span className="brand-name">NORTHBRIDGE<small>CAMPUS ATLAS</small></span>
        </a>
        <div className="topbar-right">
          <div className="server-status"><span className={serverDown ? "status-dot offline" : "status-dot"} />{serverDown ? "API OFFLINE" : "SYSTEM ONLINE"}</div>
          <div className="user-select-wrap">
            <span className="user-avatar">{selectedUser?.role?.slice(0, 1) || "U"}</span>
            <select aria-label="Switch user" value={selectedUser?.id || ""} onChange={(event) => setSelectedUserId(event.target.value)}>
              {users.map((user) => <option value={user.id} key={user.id}>{user.id} · {user.role} · limit {user.bookingLimit}</option>)}
            </select>
            <span className="select-chevron">⌄</span>
          </div>
        </div>
      </header>

      <section className="workspace">
        <div className="map-heading">
          <div><div className="eyebrow"><span className="eyebrow-line" /> LIVE RESOURCE NETWORK</div><h1>Explore <em>your campus.</em></h1></div>
          <div className="map-meta"><span>01 — 09</span><span className="meta-divider" /> NORTHBRIDGE, EST. 1987</div>
        </div>
        <div className="map-toolbar">
          <label className="search-field"><span>⌕</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search rooms, labs, books..." /><kbd>↵</kbd></label>
          <div className="toolbar-right">
            <div className="map-legend"><span><i className="legend-dot green" />Available</span><span><i className="legend-dot red" />Booked</span></div>
            <div className="feature-buttons">
              <button className={activePanel === "race" ? "feature-trigger active" : "feature-trigger"} onClick={() => setActivePanel(activePanel === "race" ? "" : "race")}>THREAD DEMO</button>
              <button className={activePanel === "logs" ? "feature-trigger active" : "feature-trigger"} onClick={() => setActivePanel(activePanel === "logs" ? "" : "logs")}>LOGS</button>
              <button className={activePanel === "oop" ? "feature-trigger active" : "feature-trigger"} onClick={() => setActivePanel(activePanel === "oop" ? "" : "oop")}>OOP MAP</button>
            </div>
          </div>
        </div>

        <div className="scene-wrap">
          {loading ? (
            <div className="state-overlay"><span className="loader-orbit" /><div>CONNECTING TO CAMPUS NETWORK</div></div>
          ) : serverDown ? (
            <div className="state-overlay offline-state"><span className="offline-symbol">⌁</span><h2>Signal lost.</h2><p>The Campus API is not responding on port 8080.</p><button className="action-button compact" onClick={() => refresh(true)}>Try again</button></div>
          ) : (
            <ResourceScene
              resources={resources}
              selectedId={selectedResource?.id}
              query={query}
              searchMatches={searchMatches}
              onSelect={selectResource}
            />
          )}
          <div className="map-coordinates">51°30'26.4"N<br />00°07'39.2"W</div>
          <div className="scene-instruction"><span>✦</span> DRAG TO EXPLORE <i /> CLICK A RESOURCE</div>
          <AnimatePresence mode="wait">
            {activePanel === "race" && <ConcurrencyDemo key="race" onClose={() => setActivePanel("")} onError={pushToast} onComplete={() => refresh()} />}
            {activePanel === "logs" && <LogsPanel key="logs" onClose={() => setActivePanel("")} onError={pushToast} />}
            {activePanel === "oop" && <OopOverlay key="oop" onClose={() => setActivePanel("")} />}
          </AnimatePresence>
          <AnimatePresence>
            {!activePanel && !loading && !serverDown && <ResourcePanel
              resource={selectedResource}
              booking={selectedBooking}
              user={selectedUser}
              onBook={book}
              onCancel={cancel}
              onClose={() => setSelectedResource(null)}
              busy={busy}
            />}
          </AnimatePresence>
        </div>

        <div className="bottom-row">
          <div className="network-note"><span className="note-icon">✳</span><span><strong>Campus, in a new dimension.</strong><small>Select any point to view availability and reserve your spot.</small></span></div>
          <BookingList bookings={bookings} resources={resources} user={selectedUser} onSelect={selectResource} onCancel={cancel} busyId={busyId} />
          <motion.button
            className={`save-button ${saveState}`}
            onClick={saveData}
            disabled={saveState === "saving"}
            whileTap={{ scale: 0.96 }}
            animate={saveState === "saved" ? { scale: [1, 1.08, 1], color: "#66efb9" } : { scale: 1 }}
          >
            {saveState === "saving" ? "SAVING..." : saveState === "saved" ? "SAVED" : "SAVE CHANGES"}
            <motion.span animate={saveState === "saved" ? { rotate: 0, scale: [1, 1.3, 1] } : { rotate: 0 }}>{saveState === "saved" ? "✓" : "↗"}</motion.span>
          </motion.button>
        </div>
      </section>

      <footer className="footer"><span>© 2026 NORTHBRIDGE UNIVERSITY</span><span>RESOURCE MANAGEMENT SYSTEM <i /> LIVE 3D MAP</span></footer>

      <Toasts toasts={toasts} />
      <AnimatePresence>
        {intro && (
          <motion.div className="intro-screen" initial={{ opacity: 1 }} exit={{ opacity: 0, transition: { duration: 0.7 } }}>
            <motion.div className="intro-content" initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ duration: 0.7 }}>
              <div className="intro-symbol"><span /><span /><span /></div>
              <p className="eyebrow">NORTHBRIDGE UNIVERSITY · 2026</p>
              <h1>Campus,<br /><em>reimagined.</em></h1>
              <p className="intro-copy">Your spaces, your resources, one living map.</p>
              <button className="enter-button" onClick={() => setIntro(false)}>ENTER THE ATLAS <span>↗</span></button>
              <small className="intro-foot">INTERACTIVE RESOURCE NETWORK · 01</small>
            </motion.div>
            <div className="intro-grid" />
          </motion.div>
        )}
      </AnimatePresence>
    </main>
  );
}
