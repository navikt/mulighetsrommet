import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Accordion, ToggleGroup } from "@navikt/ds-react";
import { ApentForInnsok } from "mulighetsrommet-api-client";
import "./ApentForInnsokToggle.module.scss";

export interface ApentForInnsokToggleProps {
  value: ApentForInnsok;

  onChange(value: ApentForInnsok): void;
}

export function ApentForInnsokToggle(props: ApentForInnsokToggleProps) {
  function onToggleChanged(value: string) {
    if (Object.values(ApentForInnsok).includes(value as ApentForInnsok)) {
      props.onChange(value as ApentForInnsok);
    }
  }

  return (
    <Accordion.Item defaultOpen={true}>
      <Accordion.Header>Åpent for innsøk</Accordion.Header>
      <Accordion.Content>
        <ToggleGroup size="small" defaultValue={props.value} onChange={onToggleChanged}>
          <ToggleGroup.Item value="APENT">Åpent</ToggleGroup.Item>
          <ToggleGroup.Item value="APENT_ELLER_STENGT">Begge</ToggleGroup.Item>
          <ToggleGroup.Item value="STENGT">
            <PadlockLockedFillIcon aria-hidden />
            Stengt
          </ToggleGroup.Item>
        </ToggleGroup>
      </Accordion.Content>
    </Accordion.Item>
  );
}
