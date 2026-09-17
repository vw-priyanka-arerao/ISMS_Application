import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Stack,
  TextField,
  Typography
} from '@mui/material';

export default function LoginForm({ onLogin, isSubmitting }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    try {
      await onLogin({ username: email, password });
    } catch (loginError) {
      setError(loginError.message || 'Unable to sign in');
    }
  }

  return (
    <Container maxWidth="sm" sx={{ py: 8 }}>
      <Card>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={3} component="form" onSubmit={handleSubmit}>
            <Box>
              <Typography variant="h4" fontWeight={700} gutterBottom>
                SecureSync AI
              </Typography>
              <Typography color="text.secondary">
                Centralized ISMS SmartFlow portal for drafting, approvals, audit readiness, and compliance visibility.
              </Typography>
            </Box>

            {error ? <Alert severity="error">{error}</Alert> : null}
            <TextField label="Email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoFocus />
            <TextField label="Password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />

            <Button type="submit" size="large" variant="contained" disabled={isSubmitting}>
              {isSubmitting ? 'Signing in...' : 'Sign in'}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Container>
  );
}

