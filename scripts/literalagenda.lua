-- literalagenda.lua - Neovim integration for Literal Agenda
-- ~/.config/nvim/lua/custom/literalagenda.lua
--
-- Requires: fzf-lua

local M = {}
local events_path    = vim.fn.expand('~/path/to/literalagenda/events/')
local repeating_path = vim.fn.expand('~/path/to/literalagenda/repeating/')

M.new = function()
  local date   = vim.fn.input('Date (YYYY-MM-DD): ', os.date('%Y-%m-%d'))
  local time   = vim.fn.input('Time (HH:MM, blank for all-day): ')
  local repeat_ = vim.fn.input('Repeat (none/weekly/monthly/yearly): ', 'none')

  local lines = { '---', 'date: ' .. date }
  if time ~= '' then table.insert(lines, 'time: ' .. time) end
  table.insert(lines, 'repeat: ' .. repeat_)
  table.insert(lines, '---')
  table.insert(lines, '')

  local filename = os.date('!%Y%m%d_%H%M%S') .. '.md'
  local filepath = events_path .. filename
  vim.fn.writefile(lines, filepath)
  vim.cmd('edit ' .. filepath)
  vim.cmd('normal! G')
end

M.list = function()
  require('fzf-lua').files({ cwd = events_path })
end

M.search = function()
  require('fzf-lua').live_grep({ cwd = events_path })
end

vim.keymap.set('n', '<leader>an', M.new,    { desc = 'New Event' })
vim.keymap.set('n', '<leader>al', M.list,   { desc = 'List Events' })
vim.keymap.set('n', '<leader>as', M.search, { desc = 'Search Events' })

return M
