import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import './App.less';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ToastContainer } from 'react-toastify';
import Routes from './Routes';
import 'react-toastify/dist/ReactToastify.css';
import { ReactQueryDevtools } from 'react-query/devtools';
import { Modal } from '@navikt/ds-react';
import { APPLICATION_NAME, MODAL_ACCESSIBILITY_WRAPPER } from './constants';

// Trengs for at tab og fokus ikke skal gå utenfor modal når den er åpen.
Modal.setAppElement?.(`#${MODAL_ACCESSIBILITY_WRAPPER}`);

const queryClient = new QueryClient({ defaultOptions: { queries: { refetchOnWindowFocus: false } } });

function App() {
  return (
    <div className="app__container">
      <div className={APPLICATION_NAME}>
        <QueryClientProvider client={queryClient}>
          <Router>
            <Routes />
          </Router>
          <ToastContainer
            position="top-right"
            autoClose={5000}
            hideProgressBar={false}
            newestOnTop
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
          />
          <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
      </div>
    </div>
  );
}

export default App;
