import { ReactElement } from "react";

interface ShowProps {
  if: boolean;
  children: ReactElement;
}

const Show = (props: ShowProps) => (props.if ? props.children : null);

export default Show;
