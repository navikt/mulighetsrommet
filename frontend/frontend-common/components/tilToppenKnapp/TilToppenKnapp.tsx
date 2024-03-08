import { useEffect, useRef, useState } from "react";
import debounce from "debounce";
import classNames from "classnames";
import { Button } from "@navikt/ds-react";
import { ArrowUpIcon } from "@navikt/aksel-icons";
import styles from "./TilToppenKnapp.module.scss";

export const TilToppenKnapp = () => {
  const [scrollPosition, setScrollPosition] = useState<number>();
  const knappRef = useRef<HTMLButtonElement>(null);

  const onScroll = debounce(() => {
    setScrollPosition(window.scrollY);
  }, 1000);

  const onClick = () => {
    if (knappSkalVises) {
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
    if (knappRef?.current) {
      knappRef.current.blur();
    }
  };

  useEffect(() => {
    window.addEventListener("scroll", onScroll);
    return () => {
      window.removeEventListener("scroll", onScroll);
    };
  }, [onScroll]);

  const knappSkalVises = scrollPosition && scrollPosition > window.innerHeight;

  return (
    <Button
      className={classNames(
        styles.til_toppen_knapp,
        styles.knapp,
        !knappSkalVises && styles.til_toppen_knapp_skjul,
      )}
      variant="secondary"
      ref={knappRef}
      hidden={!knappSkalVises}
      onClick={onClick}
      icon={<ArrowUpIcon title="Til toppen" />}
      title="Scroll tilbake til toppen"
    />
  );
};
