import { Link } from "react-router-dom";
import styles from "./Shortcuts.module.scss";

export type Shortcut = { navn: string; url: string };

interface Props {
  shortcuts: Shortcut[];
}

export function Shortcuts({ shortcuts }: Props) {
  return (
    <div>
      <ul className={styles.shortcuts_container}>
        {shortcuts.map(({ url, navn }) => (
          <li key={url + navn} className={styles.shortcut}>
            <Link to={url} data-testid={`shortcut-${url.replace("/", "")}`}>
              <span>{navn}</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
