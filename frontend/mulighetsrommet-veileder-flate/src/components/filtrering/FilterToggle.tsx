import { Accordion, ToggleGroup } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { ApentForPamelding } from "@mr/api-client";
import React from "react";
import { filterAccordionAtom, FilterAccordionTypes } from "../../core/atoms";
import { addOrRemove } from "../../utils/Utils";
import "./FilterToggle.module.scss";

export interface Props {
  accordionHeader: React.ReactNode;
  value: string;
  onChange(value: any): void;
  venstreTekst: React.ReactNode;
  midtTekst?: React.ReactNode;
  hoyreTekst: React.ReactNode;
  venstreValue: string;
  midtValue: string;
  hoyreValue: string;
  accordionIsOpenValue: FilterAccordionTypes;
}

export function FilterToggle({
  accordionHeader,
  value,
  onChange,
  venstreTekst,
  midtTekst = "Begge",
  hoyreTekst,
  venstreValue,
  midtValue,
  hoyreValue,
  accordionIsOpenValue,
}: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);
  function onToggleChanged(value: string) {
    if (Object.values(ApentForPamelding).includes(value as ApentForPamelding)) {
      onChange(value as ApentForPamelding);
    }
  }

  return (
    <Accordion.Item open={accordionsOpen.includes(accordionIsOpenValue)}>
      <Accordion.Header
        onClick={() => {
          setAccordionsOpen([...addOrRemove(accordionsOpen, accordionIsOpenValue)]);
        }}
      >
        {accordionHeader}
      </Accordion.Header>
      <Accordion.Content>
        <ToggleGroup size="small" value={value} onChange={onToggleChanged}>
          <ToggleGroup.Item value={venstreValue}>{venstreTekst}</ToggleGroup.Item>
          <ToggleGroup.Item value={midtValue}>{midtTekst}</ToggleGroup.Item>
          <ToggleGroup.Item value={hoyreValue}>{hoyreTekst}</ToggleGroup.Item>
        </ToggleGroup>
      </Accordion.Content>
    </Accordion.Item>
  );
}
