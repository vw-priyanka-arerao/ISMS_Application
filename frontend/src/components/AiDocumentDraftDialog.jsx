import { useEffect, useMemo, useState } from 'react';
import { Autocomplete, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField } from '@mui/material';

export default function AiDocumentDraftDialog({ open, api, onClose, onCreated, showMessage }) {
  const [reviewers, setReviewers] = useState([]);
  const [form, setForm] = useState({ title: '', category: 'Internal', reviewerUsername: '', prompt: '' });
  const [creating, setCreating] = useState(false);
  const selectedReviewer = useMemo(() => reviewers.find((reviewer) => reviewer.email.toLowerCase() === form.reviewerUsername.toLowerCase()), [form.reviewerUsername, reviewers]);
  const matchingReviewers = useMemo(() => {
    const query = form.reviewerUsername.trim().toLowerCase();
    return query ? reviewers.filter((reviewer) => [reviewer.email, reviewer.username, reviewer.displayName, reviewer.role].some((value) => value?.toLowerCase().includes(query))) : [];
  }, [form.reviewerUsername, reviewers]);

  useEffect(() => {
    if (open) {
      api.listUsers(true).then(setReviewers).catch((error) => showMessage(error.message || 'Unable to load reviewers', 'error'));
    }
  }, [api, open, showMessage]);

  async function createDraft() {
    setCreating(true);
    try {
      const document = await api.createAiDraft(form);
      setForm({ title: '', category: 'Internal', reviewerUsername: '', prompt: '' });
      onCreated(document);
      onClose();
    } catch (error) {
      showMessage(error.message || 'Unable to create AI draft', 'error');
    } finally {
      setCreating(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Create AI document draft</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField required label="Document title" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
          <TextField required select label="Label" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}>
            <MenuItem value="Internal">Internal</MenuItem><MenuItem value="Confidential">Confidential</MenuItem><MenuItem value="Secret">Secret</MenuItem>
          </TextField>
          <Autocomplete freeSolo options={matchingReviewers} open={Boolean(form.reviewerUsername.trim() && !selectedReviewer)} openOnFocus={false} value={selectedReviewer || form.reviewerUsername}
            onChange={(_, value) => setForm({ ...form, reviewerUsername: typeof value === 'string' ? value : value?.email || '' })}
            onInputChange={(_, value) => setForm({ ...form, reviewerUsername: value })}
            getOptionLabel={(option) => typeof option === 'string' ? option : option.email}
            renderOption={(props, reviewer) => <li {...props} key={reviewer.username}>{reviewer.email} ({reviewer.role})</li>}
            renderInput={(params) => <TextField {...params} label="Reviewer email" helperText="Start typing an email, name, or role to find a reviewer." />}
          />
          <TextField label="Draft instructions" value={form.prompt} onChange={(event) => setForm({ ...form, prompt: event.target.value })} multiline minRows={4} />
        </Stack>
      </DialogContent>
      <DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" disabled={!form.title.trim() || creating} onClick={createDraft}>Create draft</Button></DialogActions>
    </Dialog>
  );
}