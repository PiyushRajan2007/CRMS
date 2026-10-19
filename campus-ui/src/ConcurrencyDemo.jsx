import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { api } from "./api.js";

function ThreadOrb({ event, index, phase }) {
  if (!event)
    return <div className="thread-lane waiting"><span>Waiting for Java threads…</span></div>;

  const winner = Boolean(event.winner);
  const resolved = phase === "resolved";
  const label = `Thread-${index + 1}`;
  const status = phase === "approach"
    ? "RACING TO R101"
    : winner
      ? "BOOKED · WINNER"
      : resolved
        ? "NOT AVAILABLE"
        : "BLOCKED BY SYNCHRONIZED";

  return (
    <div className={`thread-lane ${winner ? "thread-winner" : "thread-loser"}`}>
      <motion.div
        className={`thread-orb ${winner ? "orb-winner" : "orb-loser"}`}
        initial={{ x: index === 0 ? -120 : 120, scale: 0.7, opacity: 0.3 }}
        animate={{
          x: phase === "approach" ? 0 : winner ? -5 : 5,
          scale: phase === "approach" ? 1 : winner ? 1.18 : 0.84,
          opacity: 1,
        }}
        transition={{ duration: 1.25, ease: [0.18, 0.76, 0.22, 1] }}
      />
      <div className="thread-copy">
        <strong>{label}</strong>
        <span>{status}</span>
        <small>{event.thread} · {event.action}</small>
      </div>
    </div>
  );
}

export default function ConcurrencyDemo({ onClose, onError, onComplete }) {
  const [events, setEvents] = useState([]);
  const [phase, setPhase] = useState("idle");
  const [running, setRunning] = useState(false);

  async function runDemo() {
    setRunning(true);
    setEvents([]);
    setPhase("idle");
    try {
      const response = await api.demoConcurrency();
      if (!Array.isArray(response)
        || response.length !== 2
        || response.some((event) => typeof event.thread !== "string" || typeof event.winner !== "boolean" || typeof event.blocked !== "boolean")
        || response.filter((event) => event.winner).length !== 1
        || !response.some((event) => !event.winner && event.blocked))
        throw new Error("The concurrency API returned an invalid event list.");
      setEvents(response);
      setPhase("approach");
      window.setTimeout(() => setPhase("locking"), 1400);
      window.setTimeout(() => {
        setPhase("resolved");
        setRunning(false);
        onComplete();
      }, 2900);
    } catch (error) {
      setRunning(false);
      onError(error);
    }
  }

  const done = phase === "resolved";
  const winnerEvent = events.find((event) => event.winner);
  const loserEvent = events.find((event) => !event.winner);

  return (
    <motion.section className="feature-panel glass concurrency-panel" initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}>
      <div className="feature-panel-head">
        <div><div className="eyebrow">JAVA · RESOURCE MANAGER</div><h2>Race for one resource</h2></div>
        <button className="icon-button panel-close" onClick={onClose} aria-label="Close concurrency demo">×</button>
      </div>
      <p className="feature-description">Two real Java threads contend for <strong>R101</strong>. The synchronized booking result drives this animation.</p>
      <div className={`race-track ${phase !== "idle" ? "race-active" : ""}`}>
        <div className="race-route" />
        <div className="race-target"><span>R101</span><i>◇</i><small>SHARED RESOURCE</small></div>
        <div className="race-lanes">
          {phase === "idle" ? (
            <>
              <div className="thread-lane"><span className="thread-orb orb-winner" /><div className="thread-copy"><strong>Thread-1</strong><span>READY TO COMPETE</span></div></div>
              <div className="thread-lane"><span className="thread-orb orb-loser" /><div className="thread-copy"><strong>Thread-2</strong><span>READY TO COMPETE</span></div></div>
            </>
          ) : events.map((event, index) => <ThreadOrb key={event.thread} event={event} index={index} phase={phase} />)}
        </div>
      </div>
      {done && (
        <motion.div className="race-outcome" initial={{ opacity: 0, y: 7 }} animate={{ opacity: 1, y: 0 }}>
          <span className="outcome-check">✓</span>
          <div><strong>{winnerEvent?.thread} booked R101</strong><small>{loserEvent?.thread} was blocked by synchronized; resource not available.</small></div>
        </motion.div>
      )}
      <div className="feature-actions">
        <button className="action-button" onClick={runDemo} disabled={running}>
          {running ? "THREADS IN MOTION..." : done ? "RUN AGAIN" : "RUN TWO-THREAD DEMO"} <span>↗</span>
        </button>
        {phase !== "idle" && <span className="event-stamp">{events.length} JAVA EVENTS · {events[0]?.timestamp?.replace("T", " ")}</span>}
      </div>
    </motion.section>
  );
}
