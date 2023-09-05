import { MouseEvent } from 'react';

interface DialogEventDetails {
  dialogId?: string;
  aktivitetId?: string;
}

export const byttTilDialogFlate = ({ event, dialogId }: { event: MouseEvent; dialogId: string }) => {
  event.preventDefault();
  window.history.pushState('', 'Dialog', `/${dialogId}`);
  window.dispatchEvent(
    new CustomEvent<DialogEventDetails>('visDialog', {
      detail: {
        dialogId,
        aktivitetId: undefined,
      },
    })
  );
};
