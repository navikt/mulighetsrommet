import styles from "./Filter.module.scss";

export const FilterTag = (props: { label?: string; onClick: () => void }) => (
  <>
    <div className={styles.tag}>
      <div className={styles.tag_inner}>
        <label className={styles.tag_label}>{props.label}</label>
        <label className={styles.tag_button} onClick={props.onClick}>
          ✕
        </label>
      </div>
    </div>
  </>
);
