import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import Decorator from './components/decorator/Decorator';
import './App.less';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ToastContainer } from 'react-toastify';
import Routes from './Routes';
import 'react-toastify/dist/ReactToastify.css';
import { ReactQueryDevtools } from 'react-query/devtools';
import 'bootstrap/dist/css/bootstrap.min.css'; // TODO: Vi må finne et bedre alternativ. Kan ikke ha 5k+ linjer med CSS kun for litt grid-system. Men det får gå for nå.
import { Modal } from '@navikt/ds-react';

const queryClient = new QueryClient({ defaultOptions: { queries: { refetchOnWindowFocus: false } } });

// Trengs for at tab og fokus ikke skal gå utenfor modal når den er åpen.
Modal.setAppElement?.('#applikasjon');

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Decorator />
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
  );
}

export default App;
