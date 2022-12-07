import { Link } from "react-router-dom";
import styles from "./Shortcuts.module.scss";
import { HiOutlineDocumentDuplicate } from "react-icons/hi";

export function Shortcuts() {
  return (
    <div>
      <ul className={styles.shortcuts_container}>
        <li className={styles.shortcut}>
          <Link to="/oversikt">
            <span>Min oversikt</span>
            <HiOutlineDocumentDuplicate />
          </Link>
          <Link to="/tiltakstyper">
            <span>Mine tiltakstyper</span>
            <HiOutlineDocumentDuplicate />
          </Link>
        </li>
      </ul>
    </div>
  );
}
