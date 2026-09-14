import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import CheckoutPage from './pages/CheckoutPage';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1a1a1a',
    },
  },
});

const App = () => {
  return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <CheckoutPage />
      </ThemeProvider>
  );
};

export default App;