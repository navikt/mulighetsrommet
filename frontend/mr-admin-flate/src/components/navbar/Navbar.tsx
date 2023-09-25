import { NavLink } from "react-router-dom";
import styles from "./Navbar.module.scss";
import classnames from "classnames";

export type Shortcut = {
  navn: string;
  url: string;
};

interface NavbarProps {
  shortcuts: Shortcut[];
}

export function Navbar({ shortcuts }: NavbarProps) {
  return (
    <div className={styles.navlink_container}>
      {shortcuts.map(({ url, navn }) => (
        <NavLink
          key={url + navn}
          to={url}
          data-testid={`tab-${url.replace("/", "")}`}
          className={({ isActive }) =>
            isActive ? classnames(styles.navlink_active, styles.navlink) : styles.navlink
          }
        >
          {navn}
        </NavLink>
      ))}
    </div>
  );
}
