import { ToastContainer, Slide } from "react-toastify";

export function AutoSaveToastContainer() {
  return (
    <ToastContainer
      position="bottom-left"
      newestOnTop={true}
      closeOnClick
      rtl={false}
      pauseOnFocusLoss
      draggable
      pauseOnHover
      transition={Slide}
    />
  );
}
