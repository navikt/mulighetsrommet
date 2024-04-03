import { Heading, HStack } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode } from "react";
import styles from "../components/detaljside/Header.module.scss";
import { kebabCase } from "mulighetsrommet-frontend-common/utils/TestUtils";

interface HeaderBannerProps {
  heading: string;
  harUndermeny?: boolean;
  ikon?: ReactNode;
}

export function HeaderBanner({ heading, harUndermeny = false, ikon }: HeaderBannerProps) {
  return (
    <div
      className={classNames(
        styles.header_container,
        !harUndermeny && styles.header_container_border,
      )}
    >
      <HStack align="center" justify="start" gap="2" wrap className={styles.header_wrapper}>
        {ikon ? <span>{ikon}</span> : null}
        <Heading level="2" size="large" data-testid={`header_${kebabCase(heading)}`}>
          {heading}
        </Heading>
      </HStack>
    </div>
  );
}
