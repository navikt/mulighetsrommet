import styles from "../pages/Page.module.scss";
import { Heading } from "@navikt/ds-react";
import classNames from "classnames";
import { kebabCase } from "../utils/Utils";

interface HeaderBannerProps {
  heading: string;
  harUndermeny?: boolean;
}

export function HeaderBanner({ heading, harUndermeny = false }: HeaderBannerProps) {
  return (
    <aside
      className={classNames(
        styles.header_container,
        !harUndermeny && styles.header_container_border,
      )}
    >
      <Heading
        level="2"
        size="large"
        className={styles.header_wrapper}
        data-testid={`header_${kebabCase(heading)}`}
      >
        {heading}
      </Heading>
    </aside>
  );
}
