import classnames from "classnames";
import FocusTrap from "focus-trap-react";
import { ReactNode, useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import styles from "./Drawer.module.scss";

interface Props {
  isOpen: boolean;
  children: ReactNode;
  className?: string;
  onClose: () => void;
  position: "top" | "right" | "bottom" | "left";
  removeWhenClosed?: boolean;
}

function createPortalRoot() {
  const drawerRoot = document.createElement("div");
  drawerRoot.setAttribute("id", "drawer-root");

  return drawerRoot;
}

// Komponent utviklet basert på https://www.letsbuildui.dev/articles/building-a-drawer-component-with-react-portals/
export function Drawer({
  isOpen,
  children,
  className,
  onClose,
  position,
  removeWhenClosed = true,
}: Props) {
  const bodyRef = useRef(document.body);
  const portalRootRef = useRef(document.getElementById("drawer-root") || createPortalRoot());

  // Append portal root on mount
  useEffect(() => {
    bodyRef.current.appendChild(portalRootRef.current);
    const portal = portalRootRef.current;
    const bodyEl = bodyRef.current;

    return () => {
      // Clean up the portal when drawer component unmounts
      portal.remove();
      // Ensure scroll overflow is removed
      bodyEl.style.overflow = "";
    };
  }, []);

  useEffect(() => {
    const updatePageScroll = () => {
      if (isOpen) {
        bodyRef.current.style.overflow = "hidden";
      } else {
        bodyRef.current.style.overflow = "auto";
      }
    };
    updatePageScroll();
  }, [isOpen]);

  useEffect(() => {
    const onKeyPress = (e: { key: string }) => {
      if (e.key === "Escape") {
        onClose();
      }
    };

    if (isOpen) {
      window.addEventListener("keyup", onKeyPress);
    }

    return () => {
      window.removeEventListener("keyup", onKeyPress);
    };
  }, [isOpen, onClose]);

  if (removeWhenClosed && !isOpen) {
    return null;
  }

  return createPortal(
    <FocusTrap active={isOpen}>
      <div
        aria-hidden={isOpen ? "false" : "true"}
        className={classnames(styles.drawer_container, {
          [styles.open]: isOpen,
          className,
        })}
      >
        <div className={classnames(styles.drawer, styles[position])} role="dialog">
          {children}
        </div>
        <div className={styles.backdrop} onClick={onClose}></div>
      </div>
    </FocusTrap>,
    portalRootRef.current,
  );
}
