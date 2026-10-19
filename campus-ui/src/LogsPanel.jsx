import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { api } from "./api.js";

export default function LogsPanel({ onClose, onError }) {
  const [lines, setLines] = useState([]);
  const [typedCount, setTypedCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    api.logs()
      .then((result) => {
        if (active) {
          setLines(Array.isArray(result) ? result : []);
          setLoading(false);
        }
      })
      .catch((error) => {
        if (active) {
          setLoading(false);
          onError(error);
        }
      });
    return () => { active = false; };
  }, [onError]);

  useEffect(() => {
    if (loading || typedCount >= lines.length)
      return undefined;
    const timer = window.setTimeout(() => setTypedCount((count) => count + 1), 260);
    return () => window.clearTimeout(timer);
  }, [lines, loading, typedCount]);

  return (
    <motion.section className="feature-panel glass logs-panel" initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}>
      <div className="feature-panel-head">
        <div><div className="eyebrow">DATA/LOGS.TXT</div><h2>System log</h2></div>
        <button className="icon-button panel-close" onClick={onClose} aria-label="Close logs">×</button>
      </div>
      <div className="terminal-window">
        <div className="terminal-bar"><span /><span /><span /><small>campus@northbridge:~</small></div>
        <div className="terminal-content">
          {loading ? <div className="terminal-muted">Connecting to storage service<span className="typing-cursor">▋</span></div> : null}
          {!loading && lines.length === 0 && <div className="terminal-muted">No log entries returned.<span className="typing-cursor">▋</span></div>}
          {lines.slice(0, typedCount).map((line, index) => (
            <motion.div className="terminal-line" key={`${index}-${line}`} initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
              <span className="terminal-index">{String(index + 1).padStart(2, "0")}</span><span className="terminal-prompt">&gt;</span>{line}
            </motion.div>
          ))}
          {!loading && typedCount < lines.length && <span className="typing-cursor">▋</span>}
        </div>
      </div>
      <p className="terminal-caption"><span className="status-dot" /> LIVE FROM JAVA STORAGE · {loading ? "READING" : `${lines.length} ENTRIES`}</p>
    </motion.section>
  );
}
