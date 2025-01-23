import { Lenke } from "@mr/api-client-v2";
import { Lenke as LenkeComponent } from "@mr/frontend-common/components/lenke/Lenke";
import { List, VStack } from "@navikt/ds-react";

interface Props {
  lenker: Lenke[];
}

export function Lenkeliste({ lenker }: Props) {
  return (
    <List as="ul">
      {lenker.map((lenke, index) => (
        <List.Item key={index}>
          <VStack>
            <LenkeComponent
              to={lenke.lenke}
              target={lenke.apneINyFane ? "_blank" : "_self"}
              rel={lenke.apneINyFane ? "noopener noreferrer" : undefined}
              isExternal={lenke.apneINyFane}
            >
              {lenke.lenkenavn} ({lenke.lenke})
            </LenkeComponent>
            {lenke.visKunForVeileder ? <small>Vises kun i Modia</small> : null}
          </VStack>
        </List.Item>
      ))}
    </List>
  );
}
