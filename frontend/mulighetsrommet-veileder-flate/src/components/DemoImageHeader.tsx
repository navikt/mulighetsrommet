import styles from "./DemoImageHeader.module.scss";

export function DemoImageHeader() {
  return import.meta.env.DEV ? (
    <img
      src="/interflatedekorator_arbmark.png"
      id="veilarbpersonflatefs-root"
      alt="veilarbpersonflate-bilde"
      className={styles.demo_image}
    />
  ) : null;
}
