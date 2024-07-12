import { useEffect, useState } from "react";
import { Button } from "@navikt/ds-react";
import { ChevronRightLastIcon } from "@navikt/aksel-icons";
import styles from "./TilToppenKnapp.module.scss";
import classNames from "classnames";

export function TilToppenKnapp() {
  const [synlig, setSynlig] = useState(false);

  const synlighet = () => {
    window.scrollY > 450 ? setSynlig(true) : setSynlig(false);
  };

  useEffect(() => {
    window.addEventListener("scroll", synlighet);
  });

  return (
    <>
      {synlig && (
        <Button
          variant="secondary"
          icon={<ChevronRightLastIcon />}
          onClick={() => {
            window.scrollTo(0, 0);
          }}
          className={classNames(styles.til_toppen_knapp, styles.knapp)}
        />
      )}
    </>
  );
}
