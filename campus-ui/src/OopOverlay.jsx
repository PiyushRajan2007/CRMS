import { motion } from "framer-motion";

const concepts = [
  ["Encapsulation", "User, ResourceManager, StorageService"],
  ["Abstraction", "User, Resource"],
  ["Inheritance", "Student, Faculty, Admin, Room, Lab, Book"],
  ["Polymorphism", "User.canBook(), Resource.toString()"],
  ["Method overriding", "Student, Faculty, Admin, Room, Lab, Book"],
  ["Interfaces", "Bookable, Searchable"],
  ["Composition", "Main, University, ResourceManager"],
  ["Exception handling", "InvalidUserException, RoomNotAvailableException, BookingNotFoundException"],
  ["Static members", "Booking.nextId"],
  ["Inner class", "Booking.Details"],
  ["Collections and generics", "ResourceManager"],
  ["Recursion", "ResourceManager.recursiveSearch()"],
  ["Concurrency", "ResourceManager.bookRoom(), waitForRoom(), cancelBooking()"],
  ["Persistence", "StorageService.save(), StorageService.load()"],
];

export default function OopOverlay({ onClose }) {
  return (
    <motion.section className="feature-panel glass oop-panel" initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}>
      <div className="feature-panel-head">
        <div><div className="eyebrow">SOURCE MAP · SRC/CAMPUS</div><h2>OOP, in the code</h2></div>
        <button className="icon-button panel-close" onClick={onClose} aria-label="Close OOP concepts">×</button>
      </div>
      <p className="feature-description">Concepts mapped to the Java classes and methods where they are implemented.</p>
      <div className="oop-list">
        {concepts.map(([concept, classes], index) => (
          <motion.article className="oop-item" key={concept} initial={{ opacity: 0, x: 8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: index * 0.025 }}>
            <span>{String(index + 1).padStart(2, "0")}</span><strong>{concept}</strong><code>{classes}</code>
          </motion.article>
        ))}
      </div>
    </motion.section>
  );
}
